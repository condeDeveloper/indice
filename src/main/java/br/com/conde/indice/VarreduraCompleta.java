package br.com.conde.indice;

/**
 * A VARREDURA COMPLETA: le a tabela inteira, pagina por pagina, e fica com o que casa.
 *
 * <p>E o JUIZ deste repositorio, e e o juiz certo e nao so o conveniente. Ela nao usa
 * indice nenhum, nao depende de estatistica, nao depende de como as linhas estao
 * guardadas, e por isso e a unica via cuja resposta nao pode estar errada por um motivo
 * sutil. Se uma via discorda dela, a errada e a via.
 *
 * <p>E ela tambem e o TETO contra o qual todas as outras sao lidas, porque o custo dela e
 * o numero de paginas da tabela e mais nada. Nao cresce com a seletividade, nao encolhe,
 * nao depende da ordem das linhas. Uma reta.
 *
 * <p>E dessa reta que sai a pergunta inteira: a partir de que seletividade a outra via
 * passa dela?
 */
public final class VarreduraCompleta implements ViaDeAcesso {

    private final Tabela tabela;

    public VarreduraCompleta(Tabela tabela) {
        this.tabela = tabela;
    }

    @Override
    public String nome() {
        return "varredura completa";
    }

    @Override
    public int[] buscar(Consulta consulta, Contador contador) {
        int[] achadas = new int[16];
        int quantas = 0;

        for (int pagina = 0; pagina < tabela.paginas(); pagina++) {
            contador.lePagina(pagina);

            int primeira = tabela.primeiraLinhaDa(pagina);
            int ultima = tabela.ultimaLinhaDa(pagina);

            for (int linha = primeira; linha <= ultima; linha++) {
                contador.examinaLinha();

                if (!consulta.casa(tabela.chaveDa(linha))) continue;

                if (quantas == achadas.length) achadas = java.util.Arrays.copyOf(achadas, quantas * 2);

                achadas[quantas++] = linha;

                contador.devolveLinha();
            }
        }

        return java.util.Arrays.copyOf(achadas, quantas);
    }
}
