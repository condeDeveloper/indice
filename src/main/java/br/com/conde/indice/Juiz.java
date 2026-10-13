package br.com.conde.indice;

/**
 * O JUIZ: a varredura completa, e ela exige CORRECAO e nao desempenho.
 *
 * <p>O que ele cobra e so isto: a via devolveu exatamente as mesmas linhas, sem faltar
 * nenhuma, sem repetir nenhuma e sem inventar nenhuma. Quantas paginas ela leu para
 * chegar la nao entra no julgamento, e e proposital: a tabela de medidas so quer dizer
 * alguma coisa depois que as vias ja concordam sobre a resposta.
 *
 * <p>Ele compara copias ORDENADAS. As vias saem em ordens diferentes de proposito, e exigir
 * uma ordem so seria cobrar uma coisa que ninguem pediu: uma consulta sem clausula de
 * ordem pode receber as linhas em qualquer ordem, e o planejador de verdade se aproveita
 * disso.
 */
public final class Juiz {

    private Juiz() {}

    /** As linhas certas, pela varredura completa. Ordenadas. */
    public static int[] esperado(Tabela tabela, Consulta consulta) {
        Contador contador = new Contador();
        contador.prepararPara(tabela.paginas());

        int[] linhas = new VarreduraCompleta(tabela).buscar(consulta, contador);

        java.util.Arrays.sort(linhas);

        return linhas;
    }

    /** O que uma via respondeu, comparado com o certo. */
    public record Veredito(String via, int devolvidas, int esperadas, boolean certa, String problema) {

        public static Veredito boa(String via, int quantas) {
            return new Veredito(via, quantas, quantas, true, "");
        }
    }

    public static Veredito julgar(ViaDeAcesso via, Consulta consulta, int[] certo, Contador contador) {
        int[] saiu = via.buscar(consulta, contador).clone();

        java.util.Arrays.sort(saiu);

        for (int i = 1; i < saiu.length; i++)
            if (saiu[i] == saiu[i - 1])
                return new Veredito(via.nome(), saiu.length, certo.length, false,
                        "devolveu a linha " + saiu[i] + " duas vezes");

        if (saiu.length != certo.length)
            return new Veredito(via.nome(), saiu.length, certo.length, false,
                    "devolveu " + saiu.length + " linhas e o certo sao " + certo.length);

        for (int i = 0; i < saiu.length; i++)
            if (saiu[i] != certo[i])
                return new Veredito(via.nome(), saiu.length, certo.length, false,
                        "na posicao " + i + " devolveu a linha " + saiu[i]
                                + " e o certo e " + certo[i]);

        return Veredito.boa(via.nome(), saiu.length);
    }

    /** Julga uma via montando o contador certo para a tabela. */
    public static Veredito julgar(ViaDeAcesso via, Tabela tabela, Consulta consulta) {
        Contador contador = new Contador();
        contador.prepararPara(tabela.paginas());

        return julgar(via, consulta, esperado(tabela, consulta), contador);
    }
}
