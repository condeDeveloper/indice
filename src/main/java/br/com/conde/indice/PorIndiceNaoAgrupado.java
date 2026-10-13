package br.com.conde.indice;

/**
 * PELO INDICE, UMA LINHA DE CADA VEZ, NA ORDEM DA CHAVE.
 *
 * <p>Esta e a via que todo mundo imagina quando diz "usar o indice": desce no indice, acha
 * a primeira entrada da faixa, e vai buscando cada linha onde ela estiver.
 *
 * <p>E ela cobra UMA PAGINA POR LINHA. Nao uma pagina por linha distinta: uma pagina por
 * linha. Como as entradas saem em ordem de chave e a tabela nao esta nessa ordem, duas
 * linhas vizinhas no indice caem em paginas quaisquer, e a pagina que acabou de vir ja
 * nao serve para a proxima.
 *
 * <p>Por isso o custo dela e linear na seletividade, enquanto o da varredura e constante.
 * Duas retas de inclinacao diferente se cruzam, e o lugar onde elas se cruzam e o assunto
 * deste repositorio.
 *
 * <h2>O que ela nao faz</h2>
 *
 * <p>Ela nao guarda a ultima pagina lida. Essa e uma escolha e nao um esquecimento: um
 * sistema de verdade tem um cache de paginas, e a diferenca entre nao ter cache nenhum e
 * ter um cache de uma pagina so aparece quando as linhas saem em ordem de pagina, que e
 * exatamente o que a via com ordenacao faz e esta aqui nao.
 */
public final class PorIndiceNaoAgrupado implements ViaDeAcesso {

    private final Tabela tabela;
    private final Indice indice;

    public PorIndiceNaoAgrupado(Tabela tabela, Indice indice) {
        this.tabela = tabela;
        this.indice = indice;
    }

    public PorIndiceNaoAgrupado(Tabela tabela) {
        this(tabela, new Indice(tabela));
    }

    @Override
    public String nome() {
        return "indice sem ordenar";
    }

    @Override
    public int[] buscar(Consulta consulta, Contador contador) {
        int[] linhas = indice.faixa(consulta, contador);

        for (int linha : linhas) {
            contador.lePagina(tabela.paginaDa(linha));
            contador.examinaLinha();
            contador.devolveLinha();
        }

        return linhas;
    }
}
