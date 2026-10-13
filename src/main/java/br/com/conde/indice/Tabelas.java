package br.com.conde.indice;

/**
 * AS TABELAS DE TESTE: as tres ordens em que as linhas podem estar.
 *
 * <p>As tres tem as MESMAS chaves, os numeros de zero ate o total de linhas menos um. A
 * unica diferenca entre elas e a ORDEM em que as linhas estao guardadas, que e a unica
 * coisa que muda quantas paginas um indice precisa pedir.
 *
 * <ul>
 *   <li>AGRUPADA: as linhas estao na ordem da chave. E o melhor caso e o unico que uma
 *       tabela pode ter, porque as linhas so cabem numa ordem.
 *   <li>ALEATORIA: embaralhada. E o caso de uma coluna qualquer que nao e a de
 *       agrupamento, que e a situacao da maioria dos indices de um sistema de verdade.
 *   <li>CORRELACIONADA: quase em ordem. Uma coluna de data de insercao num sistema que so
 *       cresce fica assim, e e por isso que ela esta aqui: e o caso comum que os dois
 *       extremos escondem.
 * </ul>
 */
public final class Tabelas {

    private Tabelas() {}

    /** As linhas na ordem da chave. */
    public static Tabela agrupada(int linhas, int linhasPorPagina) {
        int[] chaves = new int[linhas];

        for (int i = 0; i < linhas; i++) chaves[i] = i;

        return new Tabela(chaves, linhasPorPagina, true);
    }

    /** As linhas embaralhadas: a chave nao diz nada sobre onde a linha esta. */
    public static Tabela aleatoria(int linhas, int linhasPorPagina, long semente) {
        int[] chaves = new int[linhas];

        for (int i = 0; i < linhas; i++) chaves[i] = i;

        new Sorteio(semente).embaralhar(chaves);

        return new Tabela(chaves, linhasPorPagina, false);
    }

    /**
     * Quase em ordem: embaralhada dentro de janelas, nunca entre elas.
     *
     * <p>Uma linha com a chave c fica numa linha a menos de uma janela de distancia de c.
     * Isso e o que um sistema de verdade chama de correlacao, e e o que acontece sozinho
     * com uma coluna de data quando as linhas sao inseridas conforme o tempo passa.
     */
    public static Tabela correlacionada(int linhas, int linhasPorPagina, int janela, long semente) {
        if (janela < 1) throw new IllegalArgumentException("janela precisa ser positiva");

        int[] chaves = new int[linhas];

        for (int i = 0; i < linhas; i++) chaves[i] = i;

        Sorteio sorteio = new Sorteio(semente);

        for (int inicio = 0; inicio < linhas; inicio += janela) {
            int fim = Math.min(linhas, inicio + janela);

            for (int i = fim - 1; i > inicio; i--) {
                int j = inicio + sorteio.ate(i - inicio + 1);

                int guarda = chaves[i];
                chaves[i] = chaves[j];
                chaves[j] = guarda;
            }
        }

        return new Tabela(chaves, linhasPorPagina, janela == 1);
    }
}
