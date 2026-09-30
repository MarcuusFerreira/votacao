package br.com.marcusferreira.voting.screen;

import br.com.marcusferreira.voting.agenda.Agenda;
import br.com.marcusferreira.voting.common.VotingProperties;
import br.com.marcusferreira.voting.vote.VoteOption;
import br.com.marcusferreira.voting.vote.VotingResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Builds every screen of the voting journey (Annex 1). Controllers only decide which screen to
 * show; texts, inputs and callback URLs live here.
 */
@Component
public class VotingScreens {

    public static final int DEFAULT_PAGE_SIZE = 20;

    private final ScreenUrls urls;
    private final VotingProperties properties;

    public VotingScreens(ScreenUrls urls, VotingProperties properties) {
        this.urls = urls;
        this.properties = properties;
    }

    public SelectionScreen agendaList(Page<Agenda> agendas, int page, int size) {
        List<SelectionItem> items = new ArrayList<>();
        items.add(new SelectionItem("Nova pauta", urls.newAgendaForm()));
        agendas.forEach(agenda -> items.add(new SelectionItem(agenda.getTitle(), urls.agenda(agenda.getId()))));
        if (agendas.hasNext()) {
            items.add(new SelectionItem("Próxima página", urls.agendasPage(page + 1, size)));
        }
        return new SelectionScreen("Pautas", items);
    }

    public FormScreen newAgendaForm() {
        return new FormScreen("Nova pauta",
            List.of(
                FormItem.inputText("titulo", "Título", ""),
                FormItem.inputText("descricao", "Descrição", "")),
            new ScreenButton("Cadastrar", urls.agendas(), null),
            backToList());
    }

    public FormScreen openSessionForm(Agenda agenda) {
        List<FormItem> items = new ArrayList<>();
        if (agenda.getDescription() != null && !agenda.getDescription().isBlank()) {
            items.add(FormItem.text(agenda.getDescription()));
        }
        items.add(FormItem.inputNumber("duracaoSegundos", "Duração da sessão (segundos)",
            properties.session().defaultDuration().toSeconds()));
        return new FormScreen(agenda.getTitle(), items,
            new ScreenButton("Abrir sessão", urls.openSession(agenda.getId()), null),
            backToList());
    }

    public SelectionScreen voteOptions(Agenda agenda) {
        String voteForm = urls.voteForm(agenda.getId());
        return new SelectionScreen(agenda.getTitle(), List.of(
            new SelectionItem(label(VoteOption.YES), voteForm, Map.of("voto", "SIM")),
            new SelectionItem(label(VoteOption.NO), voteForm, Map.of("voto", "NAO"))));
    }

    public FormScreen sessionClosed(Agenda agenda) {
        return new FormScreen(agenda.getTitle(),
            List.of(FormItem.text("Sessão encerrada. Consulte o resultado.")),
            new ScreenButton("Ver resultado", urls.result(agenda.getId()), null),
            backToList());
    }

    public FormScreen voteForm(Agenda agenda, VoteOption vote) {
        return new FormScreen(agenda.getTitle(),
            List.of(
                FormItem.text("Seu voto: " + label(vote)),
                FormItem.inputText("associadoId", "Identificação do associado", ""),
                FormItem.inputText("cpf", "CPF (somente números)", "")),
            new ScreenButton("Confirmar voto", urls.votes(agenda.getId()), Map.of("voto", vote == VoteOption.YES ? "SIM" : "NAO")),
            new ScreenButton("Voltar", urls.agenda(agenda.getId()), null));
    }

    public FormScreen voteRegistered(Long agendaId, VoteOption vote) {
        return new FormScreen("Voto registrado",
            List.of(FormItem.text("Seu voto (" + label(vote) + ") foi registrado.")),
            new ScreenButton("Ver resultado", urls.result(agendaId), null),
            backToList());
    }

    public FormScreen resultInProgress(Long agendaId, Instant closesAt) {
        return new FormScreen("Resultado",
            List.of(FormItem.text("Sessão em andamento até " + closesAt)),
            new ScreenButton("Atualizar", urls.result(agendaId), null),
            backToList());
    }

    public FormScreen result(VotingResult result) {
        String text = result.hasVotes()
            ? "Sim: %d / Não: %d — Vencedor: %s".formatted(result.yesVotes(), result.noVotes(), result.winner().label())
            : "Sim: 0 / Não: 0 — Nenhum voto registrado";
        return new FormScreen("Resultado", List.of(FormItem.text(text)),
            new ScreenButton("Voltar às pautas", urls.agendasPage(0, DEFAULT_PAGE_SIZE), null),
            null);
    }

    private ScreenButton backToList() {
        return new ScreenButton("Voltar", urls.agendasPage(0, DEFAULT_PAGE_SIZE), null);
    }

    private static String label(VoteOption vote) {
        return vote == VoteOption.YES ? "Sim" : "Não";
    }
}
