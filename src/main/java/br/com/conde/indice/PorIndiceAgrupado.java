package br.com.conde.indice;

/**
 * PELO INDICE AGRUPADO: usa o indice uma vez so, para achar onde comecar.
 *
 * <p>Numa tabela agrupada as linhas estao guardadas na ordem da chave. Entao as linhas que
 * casam com uma faixa sao CONTIGUAS, e depois de achar a primeira o indice nao serve mais
 * para nada: basta ir andando na tabela ate a chave passar do fim da faixa.
 *
 * <p>E dai vem o custo: o numero de linhas que casam dividido pelo tamanho da pagina, mais
 * uma. Nao uma pagina por linha. Com cem linhas por pagina, cem linhas que casam custam
 * duas paginas em vez de cem.
 *
 * <p>Ela tambem le muito menos ENTRADAS DE INDICE do que as outras duas, e isso nao e
 * detalhe: as outras leem uma entrada por linha devolvida, e esta le so a descida. Uma
 * consulta que devolve cem mil linhas le dezessete entradas aqui e cem mil la.
 *
 * <h2>Por que nao usar isso em tudo</h2>
 *
 * <p>Porque as linhas so cabem numa ordem. Uma tabela tem no maximo UM indice agrupado, e
 * escolher qual coluna agrupa e escolher qual consulta vai ser rapida. Todas as outras
 * colunas indexadas caem nas vias de cima, e e por isso que elas sao o caso comum e esta
 * e o caso de sorte.
 */
public final class PorIndiceAgrupado implements ViaDeAcesso {

    private final Tabela tabela;
    private final Indice indice;

    public PorIndiceAgrupado(Tabela tabela, Indice indice) {
        if (!tabela.agrupada())
            throw new IllegalArgumentException(
                    "esta via so existe para tabela agrupada, e a ordem das linhas e da tabela");

        this.tabela = tabela;
        this.indice = indice;
    }

    public PorIndiceAgrupado(Tabela tabela) {
        this(tabela, new Indice(tabela));
    }

    @Override
    public String nome() {
        return "indice agrupado";
    }

    @Override
    public int[] buscar(Consulta consulta, Contador contador) {
        int posicao = indice.primeiraComChaveAPartirDe(consulta.de(), contador);

        if (posicao == indice.entradas()) return new int[0];

        contador.leEntrada();

        int[] achadas = new int[16];
        int quantas = 0;

        int paginaAnterior = -1;

        /* Daqui em diante o indice nao e mais consultado: a tabela esta na ordem dele. */
        for (int linha = indice.linhaDaEntrada(posicao); linha < tabela.linhas(); linha++) {
            int pagina = tabela.paginaDa(linha);

            if (pagina != paginaAnterior) {
                contador.lePagina(pagina);
                paginaAnterior = pagina;
            }

            contador.examinaLinha();

            if (!consulta.casa(tabela.chaveDa(linha))) break;

            if (quantas == achadas.length) achadas = java.util.Arrays.copyOf(achadas, quantas * 2);

            achadas[quantas++] = linha;

            contador.devolveLinha();
        }

        return java.util.Arrays.copyOf(achadas, quantas);
    }
}
