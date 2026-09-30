// Load test for the voting API.
//
// setup():    creates an agenda and opens a voting session through the API.
// default():  each iteration casts one vote from a distinct member (alternating SIM/NAO).
// teardown(): re-votes with an existing member (expects 409), waits for the session to
//             close and checks that the tallied result matches the votes accepted.
//
// Environment variables:
//   BASE_URL         API address (default http://localhost:8080)
//   VOTES            total votes to cast (default 1000)
//   VUS              concurrent virtual users (default 50)
//   SESSION_SECONDS  voting session duration; votes must finish before it closes (default 30)
import http from 'k6/http';
import exec from 'k6/execution';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VOTES = parseInt(__ENV.VOTES || '1000', 10);
const VUS = parseInt(__ENV.VUS || '50', 10);
const SESSION_SECONDS = parseInt(__ENV.SESSION_SECONDS || '30', 10);

const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

const votesAccepted = new Counter('votes_accepted');
const votesRejected = new Counter('votes_rejected');

export const options = {
    scenarios: {
        votes: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: VOTES,
            maxDuration: `${Math.max(SESSION_SECONDS - 5, 5)}s`,
        },
    },
    teardownTimeout: `${SESSION_SECONDS + 30}s`,
    thresholds: {
        http_req_failed: ['rate<0.01'],
        'http_req_duration{name:cast_vote}': ['p(95)<500'],
        checks: ['rate>0.99'],
    },
};

export function setup() {
    const agenda = http.post(`${BASE_URL}/api/v1/pautas`, JSON.stringify({
        titulo: 'Pauta de carga (k6)',
        descricao: `${VOTES} votos com ${VUS} usuários virtuais`,
    }), JSON_HEADERS);
    if (agenda.status !== 201) {
        exec.test.abort(`Could not create agenda: HTTP ${agenda.status} ${agenda.body}`);
    }
    // Creation answers with a screen; the new agenda's URL comes in the Location header.
    const location = agenda.headers['Location'];
    const agendaId = location.substring(location.lastIndexOf('/') + 1);

    const openedAt = Date.now();
    const session = http.post(`${BASE_URL}/api/v1/pautas/${agendaId}/sessoes`,
        JSON.stringify({ duracaoSegundos: SESSION_SECONDS }), JSON_HEADERS);
    if (session.status !== 201) {
        exec.test.abort(`Could not open session: HTTP ${session.status} ${session.body}`);
    }

    const closesAt = openedAt + SESSION_SECONDS * 1000;
    console.log(`Agenda ${agendaId}: session open until ${new Date(closesAt).toISOString()}`);
    return { agendaId, closesAt };
}

export default function (data) {
    const i = exec.scenario.iterationInTest;
    const res = http.post(`${BASE_URL}/api/v1/pautas/${data.agendaId}/votos`, JSON.stringify({
        associadoId: `carga-k6-${i}`,
        cpf: '12345678900',
        voto: i % 2 === 0 ? 'SIM' : 'NAO',
    }), { ...JSON_HEADERS, tags: { name: 'cast_vote' } });

    const ok = check(res, { 'vote accepted (201)': (r) => r.status === 201 });
    (ok ? votesAccepted : votesRejected).add(1);
}

export function teardown(data) {
    const duplicate = http.post(`${BASE_URL}/api/v1/pautas/${data.agendaId}/votos`, JSON.stringify({
        associadoId: 'carga-k6-0',
        cpf: '12345678900',
        voto: 'SIM',
    }), { ...JSON_HEADERS, responseCallback: http.expectedStatuses(409), tags: { name: 'duplicate_vote' } });
    check(duplicate, { 'duplicate vote rejected (409)': (r) => r.status === 409 });

    const waitSeconds = Math.ceil((data.closesAt - Date.now()) / 1000) + 1;
    if (waitSeconds > 0) {
        console.log(`Waiting ${waitSeconds}s for the session to close...`);
        sleep(waitSeconds);
    }

    const result = http.get(`${BASE_URL}/api/v1/pautas/${data.agendaId}/resultado`, { tags: { name: 'result' } });
    const text = result.json('itens.0.texto');
    console.log(`Result for agenda ${data.agendaId}: ${text}`);

    const match = /Sim: (\d+) \/ Não: (\d+)/.exec(text || '');
    const tallied = match ? parseInt(match[1], 10) + parseInt(match[2], 10) : -1;
    check(tallied, { 'tally matches votes cast': (total) => total === VOTES });
}
