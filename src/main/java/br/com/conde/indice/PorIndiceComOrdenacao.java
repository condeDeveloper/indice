package br.com.conde.indice;

/**
 * PELO INDICE, MAS ORDENANDO AS LINHAS ANTES DE IR BUSCA-LAS.
 *
 * <p>O indice devolve numeros de linha em ordem de chave. Esta via guarda todos, ORDENA
 * por numero de linha, e so entao vai a tabela. Como a pagina de uma linha e o numero dela
 * dividido pelo tamanho da pagina, ordenar por linha e ordenar por pagina: cada pagina e
 * pedida uma vez so e nunca mais.
 *
 * <p>E o que um sistema de verdade chama de varredura por mapa de bits. O nome vem do jeito
 * de guardar os numeros de linha, mas o ganho nao vem do mapa de bits: vem da ordem.
 *
 * <p>O teto dela e o numero de paginas da tabela, porque nao ha como pedir mais paginas
 * distintas do que existem. Isso tem uma consequencia forte: por mais seletiva que a
 * consulta deixe de ser, esta via nunca le mais paginas do que a varredura completa.
 *
 * <h2>O preco</h2>
 *
 * <p>Ela troca paginas por processador e por memoria. Precisa dos numeros de linha todos
 * na mao antes de ler a primeira linha, e precisa ordenar. E perde a ordem de chave: quem
 * pediu os dados ordenados vai ter que ordenar de novo.
 */
public final class PorIndiceComOrdenacao implements ViaDeAcesso {

    private final Tabela tabela;
    private final Indice indice;

    public PorIndiceComOrdenacao(Tabela tabela, Indice indice) {
        this.tabela = tabela;
        this.indice = indice;
    }

    public PorIndiceComOrdenacao(Tabela tabela) {
        this(tabela, new Indice(tabela));
    }

    @Override
    public String nome() {
        return "indice ordenando";
    }

    @Override
    public int[] buscar(Consulta consulta, Contador contador) {
        int[] linhas = indice.faixa(consulta, contador);

        java.util.Arrays.sort(linhas);

        contador.ordena(linhas.length);

        int paginaAnterior = -1;

        for (int linha : linhas) {
            int pagina = tabela.paginaDa(linha);

            /* A pagina so e pedida quando muda, e depois de ordenar ela nunca volta. */
            if (pagina != paginaAnterior) {
                contador.lePagina(pagina);
                paginaAnterior = pagina;
            }

            contador.examinaLinha();
            contador.devolveLinha();
        }

        return linhas;
    }
}
