package br.com.marcusferreira.voting.vote;

public record VotingResult(long yesVotes, long noVotes) {

    public enum Winner {
        YES("SIM"), NO("NAO"), TIE("EMPATE");

        private final String label;

        Winner(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public boolean hasVotes() {
        return yesVotes + noVotes > 0;
    }

    public Winner winner() {
        if (yesVotes == noVotes) {
            return Winner.TIE;
        }
        return yesVotes > noVotes ? Winner.YES : Winner.NO;
    }
}
