package br.com.conde.indice;

/**
 * O CONTADOR: paginas lidas. Nunca segundos.
 *
 * <p>A PAGINA LIDA e a medida certa e nao so a conveniente. Um banco de dados nao le
 * linhas, le BLOCOS: trazer um bloco do disco custa o mesmo com uma linha util dentro ou
 * com cem. Toda a decisao de usar ou nao usar um indice e sobre quantos blocos cada
 * caminho traz.
 *
 * <p>E a medida tem um teto conhecido, que e o numero de paginas da tabela. Isso e o que
 * torna a coluna legivel sem comparar com nada: ler 1.000 paginas numa tabela de 1.000 e
 * uma frase completa, e ler 9.512 e outra.
 *
 * <h2>A pagina distinta</h2>
 *
 * <p>Ha duas contas e a diferenca entre elas e uma das tabelas. A PAGINA LIDA conta toda
 * vez que o caminho pede um bloco; a PAGINA DISTINTA conta quantos blocos diferentes ele
 * pediu.
 *
 * <p>Um indice nao agrupado pede a mesma pagina varias vezes, porque as linhas que casam
 * estao espalhadas e ele vai buscando uma a uma na ordem da chave. Ordenar os pedidos
 * antes faz as duas colunas se encontrarem, e e disso que a tabela da ordenacao trata.
 *
 * <p>Segundos nao serviriam: a integracao continua roda em tres sistemas, e uma leitura
 * sequencial de disco e varias vezes mais rapida que uma aleatoria pelo mesmo numero de
 * blocos. Essa diferenca e real e e outra conta; aqui a pergunta e quantos blocos.
 */
public final class Contador {

    private long paginasLidas;
    private long entradasDeIndice;
    private long linhasExaminadas;
    private long linhasDevolvidas;
    private long passosDeOrdenacao;

    private boolean[] jaLida;
    private long paginasDistintas;

    /**
     * Prepara a contagem de paginas distintas para uma tabela com tantas paginas.
     *
     * <p>E obrigatorio. Sem isso nao ha onde marcar quais paginas ja vieram, e a coluna de
     * paginas distintas sairia zerada em silencio, que e o pior jeito de uma medida estar
     * errada.
     */
    public void prepararPara(int paginas) {
        if (paginas < 1) throw new IllegalArgumentException("pelo menos uma pagina");

        jaLida = new boolean[paginas];
        paginasDistintas = 0;
    }

    public void lePagina(int pagina) {
        if (jaLida == null)
            throw new IllegalStateException("chame prepararPara antes de contar paginas");

        if (pagina < 0 || pagina >= jaLida.length)
            throw new IllegalArgumentException(
                    "pagina " + pagina + " fora da tabela, que tem " + jaLida.length);

        paginasLidas++;

        if (jaLida[pagina]) return;

        jaLida[pagina] = true;
        paginasDistintas++;
    }

    /** Uma entrada do indice foi percorrida. */
    public void leEntrada() {
        entradasDeIndice++;
    }

    /** Uma linha foi tirada da pagina e olhada. */
    public void examinaLinha() {
        linhasExaminadas++;
    }

    public void devolveLinha() {
        linhasDevolvidas++;
    }

    /**
     * Ordenar tantos numeros de linha custa tantos passos.
     *
     * <p>Esta conta existe para que a via que ordena antes de buscar nao pareca de graca.
     * Ela troca paginas por processador, e sem esta coluna a troca ficaria invisivel.
     */
    public void ordena(int quantos) {
        if (quantos < 2) return;

        passosDeOrdenacao += (long) quantos * (64 - Long.numberOfLeadingZeros(quantos - 1));
    }

    public long paginasLidas() {
        return paginasLidas;
    }

    public long paginasDistintas() {
        return paginasDistintas;
    }

    public long entradasDeIndice() {
        return entradasDeIndice;
    }

    public long linhasExaminadas() {
        return linhasExaminadas;
    }

    public long linhasDevolvidas() {
        return linhasDevolvidas;
    }

    public long passosDeOrdenacao() {
        return passosDeOrdenacao;
    }

    /** Quantas vezes a mesma pagina foi pedida de novo. Zero quer dizer nenhuma repeticao. */
    public long paginasRepetidas() {
        return paginasLidas - paginasDistintas;
    }
}
