package br.com.conde.indice;

/**
 * A TABELA: linhas com uma chave, guardadas em PAGINAS de tamanho fixo.
 *
 * <p>A pagina e a unidade de leitura e e por isso que ela existe aqui. Uma tabela guardada
 * como um vetor de linhas, sem paginas, faria todo caminho de acesso custar o mesmo, e nao
 * haveria assunto nenhum.
 *
 * <p>A linha de numero i mora na pagina i dividido pelo tamanho da pagina. Essa e a unica
 * coisa que o leitor precisa saber sobre o armazenamento, e e dela que sai tudo: linhas
 * vizinhas estao na mesma pagina, e linhas espalhadas estao em paginas diferentes.
 *
 * <h2>Agrupada ou nao</h2>
 *
 * <p>Uma tabela esta AGRUPADA por uma chave quando as linhas estao guardadas na ordem dela.
 * Isso nao e uma propriedade do indice, e da tabela: as linhas so podem estar numa ordem,
 * e por isso so pode haver um indice agrupado.
 *
 * <p>E e a diferenca mais cara deste repositorio. Numa tabela agrupada, as linhas que casam
 * com uma faixa estao contiguas e cabem em poucas paginas; numa nao agrupada, cada uma
 * pode estar numa pagina diferente.
 */
public final class Tabela {

    private final int[] chaves;
    private final int linhasPorPagina;
    private final boolean agrupada;

    public Tabela(int[] chaves, int linhasPorPagina, boolean agrupada) {
        if (linhasPorPagina < 1) throw new IllegalArgumentException("pelo menos uma linha por pagina");
        if (chaves.length == 0) throw new IllegalArgumentException("tabela vazia");

        this.chaves = chaves.clone();
        this.linhasPorPagina = linhasPorPagina;
        this.agrupada = agrupada;

        if (!agrupada) return;

        for (int i = 1; i < this.chaves.length; i++)
            if (this.chaves[i] < this.chaves[i - 1])
                throw new IllegalArgumentException(
                        "uma tabela agrupada precisa estar em ordem de chave");
    }

    /** A mesma tabela com as linhas ordenadas pela chave, que e o que agrupar quer dizer. */
    public static Tabela agrupando(int[] chaves, int linhasPorPagina) {
        int[] ordenadas = chaves.clone();

        java.util.Arrays.sort(ordenadas);

        return new Tabela(ordenadas, linhasPorPagina, true);
    }

    public int linhas() {
        return chaves.length;
    }

    public int linhasPorPagina() {
        return linhasPorPagina;
    }

    public boolean agrupada() {
        return agrupada;
    }

    public int chaveDa(int linha) {
        return chaves[linha];
    }

    public int paginaDa(int linha) {
        return linha / linhasPorPagina;
    }

    /** Quantas paginas a tabela ocupa. E o teto de toda coluna deste repositorio. */
    public int paginas() {
        return (chaves.length + linhasPorPagina - 1) / linhasPorPagina;
    }

    public int primeiraLinhaDa(int pagina) {
        return pagina * linhasPorPagina;
    }

    public int ultimaLinhaDa(int pagina) {
        return Math.min(chaves.length, (pagina + 1) * linhasPorPagina) - 1;
    }
}
