package br.com.conde.indice;

/**
 * O INDICE: as chaves em ordem, cada uma com o numero da linha onde ela mora.
 *
 * <p>Esta e a forma mais simples que ainda e honesta: um vetor ordenado por chave com
 * busca binaria. Uma arvore B tem outra altura e outro custo por no, mas o que este
 * repositorio mede nao muda, porque o assunto nao e como chegar na primeira entrada: e
 * quantas PAGINAS DA TABELA a lista de linhas faz ler depois.
 *
 * <p>O indice nao guarda as linhas, guarda ponteiros para elas. Essa indirecao e a origem
 * de tudo: quem pergunta ao indice recebe numeros de linha espalhados, e tem que ir buscar
 * cada um onde ele estiver.
 *
 * <p>As entradas estao ordenadas por chave e, em caso de empate, por linha. O desempate
 * por linha existe para que a resposta nao dependa de como a ordenacao se comportou, e
 * para que o juiz possa comparar vetores em vez de conjuntos.
 */
public final class Indice {

    private final int[] chaves;
    private final int[] linhas;

    public Indice(Tabela tabela) {
        int n = tabela.linhas();

        long[] par = new long[n];

        for (int linha = 0; linha < n; linha++) {
            int chave = tabela.chaveDa(linha);

            if (chave < 0) throw new IllegalArgumentException("este indice so aceita chave nao negativa");

            /* Chave nos 32 bits de cima e linha nos de baixo: uma ordenacao de longos ja
             * ordena por chave e desempata por linha, sem comparador nenhum. */
            par[linha] = ((long) chave << 32) | linha;
        }

        java.util.Arrays.sort(par);

        this.chaves = new int[n];
        this.linhas = new int[n];

        for (int i = 0; i < n; i++) {
            this.chaves[i] = (int) (par[i] >>> 32);
            this.linhas[i] = (int) par[i];
        }
    }

    public int entradas() {
        return chaves.length;
    }

    public int chaveDaEntrada(int posicao) {
        return chaves[posicao];
    }

    public int linhaDaEntrada(int posicao) {
        return linhas[posicao];
    }

    /**
     * Os numeros de linha que casam com a consulta, NA ORDEM DO INDICE.
     *
     * <p>Em ordem do indice quer dizer em ordem de chave, que numa tabela nao agrupada nao
     * tem nada a ver com a ordem das paginas. Essa frase e o repositorio inteiro.
     */
    public int[] faixa(Consulta consulta, Contador contador) {
        int inicio = primeiraComChaveAPartirDe(consulta.de(), contador);

        int fim = inicio;

        while (fim < chaves.length && chaves[fim] <= consulta.ate()) {
            contador.leEntrada();
            fim++;
        }

        /* A entrada que falhou tambem foi lida: e assim que o indice sabe onde parar. */
        if (fim < chaves.length) contador.leEntrada();

        int[] resposta = new int[fim - inicio];

        System.arraycopy(linhas, inicio, resposta, 0, resposta.length);

        return resposta;
    }

    /** Busca binaria, cobrando uma entrada por comparacao. E o custo de descer no indice. */
    public int primeiraComChaveAPartirDe(int chave, Contador contador) {
        int baixo = 0;
        int alto = chaves.length;

        while (baixo < alto) {
            int meio = (baixo + alto) >>> 1;

            contador.leEntrada();

            if (chaves[meio] < chave) baixo = meio + 1;
            else alto = meio;
        }

        return baixo;
    }
}
