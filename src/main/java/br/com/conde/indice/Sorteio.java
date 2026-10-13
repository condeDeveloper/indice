package br.com.conde.indice;

/**
 * O SORTEIO DA CASA: um gerador linear congruente, escrito aqui de proposito.
 *
 * <p>Ele existe para que a mesma semente de a mesma tabela em qualquer maquina e em
 * qualquer versao de Java. A biblioteca padrao nao promete isso entre versoes, e um
 * repositorio cujo assunto sao numeros escritos no README nao pode depender disso.
 *
 * <p>Sao as constantes de Knuth, as mesmas do gerador de 64 bits que todo mundo usa.
 *
 * <h2>Por que o deslocamento de 32</h2>
 *
 * <p>Num gerador linear congruente os bits de baixo sao fracos de um jeito exato e nao
 * estatistico: o ultimo bit alterna com periodo dois, o penultimo com periodo quatro, e
 * assim por diante. Devolver o estado inteiro entregaria essa estrutura a quem usasse o
 * sorteio.
 *
 * <p>Aqui vale dizer o que eu MEDI, porque a frase de sempre sobre isso nao se confirmou
 * toda. Trocando o deslocamento por um corte nos 32 bits de baixo, o embaralhamento de
 * Fisher e Yates da tabela de cem mil linhas continua bom: a consulta de um por cento le
 * 613 paginas com o gerador estragado e 630 com o certo. Fisher e Yates pede um resto por
 * um limite diferente a cada passo, e isso sozinho dissolve o padrao.
 *
 * <p>O que quebra de verdade e o sorteio com limite FIXO e potencia de dois, e esse caso
 * existe neste repositorio: a tabela correlacionada embaralha dentro de janelas, e com
 * janela dois ela pede ate(2) o tempo todo. Com os bits de baixo, ate(2) alterna zero, um,
 * zero, um, para sempre, e a janela passa a trocar exatamente um par sim e um par nao. Ha
 * um teste so sobre isso.
 *
 * <p>Entao a razao do deslocamento nao e que todo uso quebraria: e que eu nao consigo
 * auditar todo uso futuro, e um gerador cujo defeito depende de qual limite o chamador
 * escolheu e uma armadilha. Os 32 bits de cima nao tem esse defeito.
 */
public final class Sorteio {

    private long estado;

    public Sorteio(long semente) {
        this.estado = semente;
    }

    /** Um inteiro de 32 bits, tirado da metade de cima do estado. */
    public int proximo() {
        estado = estado * 6364136223846793005L + 1442695040888963407L;

        return (int) (estado >>> 32);
    }

    /** Um inteiro de 0 inclusive ate o limite exclusive. */
    public int ate(int limite) {
        if (limite < 1) throw new IllegalArgumentException("limite precisa ser positivo");

        return Math.floorMod(proximo(), limite);
    }

    /** Embaralha o vetor no lugar, pelo metodo de Fisher e Yates. */
    public void embaralhar(int[] vetor) {
        for (int i = vetor.length - 1; i > 0; i--) {
            int j = ate(i + 1);

            int guarda = vetor[i];
            vetor[i] = vetor[j];
            vetor[j] = guarda;
        }
    }
}
