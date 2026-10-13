package br.com.conde.indice;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("correcao") || comando.equals("tudo")) correcao();
        if (comando.equals("seletividade") || comando.equals("tudo")) seletividade();
        if (comando.equals("agrupamento") || comando.equals("tudo")) agrupamento();
        if (comando.equals("ordenacao") || comando.equals("tudo")) ordenacao();
        if (comando.equals("pagina") || comando.equals("tudo")) pagina();
    }

    private static void correcao() {
        System.out.println();
        System.out.println("TODA VIA DEVOLVE AS MESMAS LINHAS? O juiz e a varredura completa.");
        System.out.println();

        var linhas = Medidas.aCorrecao();
        long erradas = linhas.stream().filter(l -> !l.certa()).count();

        System.out.printf("  %-14s %-22s %12s %10s %s%n",
                "tabela", "via", "linhas", "confere", "problema");

        for (var l : linhas)
            System.out.printf("  %-14s %-22s %12d %10s %s%n",
                    l.tabela(), l.via(), l.linhas(), l.certa() ? "sim" : "NAO", l.problema());

        System.out.println();
        System.out.printf("  %d combinacoes, %d erradas.%n", linhas.size(), erradas);
        System.out.println();
    }

    private static void seletividade() {
        System.out.println();
        System.out.println("PAGINAS LIDAS POR SELETIVIDADE, numa tabela de " + Medidas.LINHAS
                + " linhas com " + Medidas.LINHAS_POR_PAGINA + " por pagina.");
        System.out.println("A varredura completa le " + Medidas.LINHAS / Medidas.LINHAS_POR_PAGINA
                + " paginas, sempre. Essa e a reta de comparacao.");
        System.out.println();

        System.out.printf("  %12s %10s %-22s %12s %14s %18s%n",
                "seletividade", "linhas", "via", "paginas", "entradas", "vezes a varredura");

        for (var l : Medidas.aSeletividade())
            System.out.printf(Locale.ROOT, "  %12.4f %10d %-22s %12d %14d %18.3f%n",
                    l.seletividade(), l.linhas(), l.via(), l.paginas(), l.entradas(),
                    l.vezesAVarredura());

        System.out.println();
    }

    private static void agrupamento() {
        System.out.println();
        System.out.println("A MESMA CONSULTA, AS MESMAS CHAVES, SO MUDANDO A ORDEM DAS LINHAS.");
        System.out.println("Janela n quer dizer que a linha da chave c esta a menos de n linhas de c.");
        System.out.println();

        System.out.printf("  %-14s %10s %14s %14s %14s %18s%n",
                "forma", "linhas", "sem ordenar", "ordenando", "distintas", "vezes a varredura");

        for (var l : Medidas.oAgrupamento())
            System.out.printf(Locale.ROOT, "  %-14s %10d %14d %14d %14d %18.3f%n",
                    l.forma(), l.linhas(), l.semOrdenar(), l.ordenando(), l.distintas(),
                    l.vezesAVarredura());

        System.out.println();
    }

    private static void ordenacao() {
        System.out.println();
        System.out.println("O QUE ORDENAR OS NUMEROS DE LINHA ANTES DE BUSCA-LAS ECONOMIZA.");
        System.out.println();

        System.out.printf("  %12s %10s %14s %14s %14s %16s %14s%n",
                "seletividade", "linhas", "sem ordenar", "ordenando", "repetidas",
                "fracao da tabela", "passos");

        for (var l : Medidas.aOrdenacao())
            System.out.printf(Locale.ROOT, "  %12.4f %10d %14d %14d %14d %16.4f %14d%n",
                    l.seletividade(), l.linhas(), l.semOrdenar(), l.ordenando(), l.repetidas(),
                    l.fracaoDaTabela(), l.passos());

        System.out.println();
    }

    private static void pagina() {
        System.out.println();
        System.out.println("ONDE O INDICE SEM ORDENAR PASSA DA VARREDURA, por tamanho de pagina.");
        System.out.println();

        System.out.printf("  %10s %12s %10s %14s %14s %14s %20s%n",
                "linhas", "por pagina", "paginas", "virada linhas", "virada fracao",
                "1 / por pagina", "lido ordenando ali");

        for (var l : Medidas.aPagina())
            System.out.printf(Locale.ROOT, "  %10d %12d %10d %14d %14.5f %14.5f %20.4f%n",
                    l.linhas(), l.linhasPorPagina(), l.paginasDaTabela(), l.viradaEmLinhas(),
                    l.viradaEmFracao(), l.umSobreAPagina(), l.fracaoLidaNaVirada());

        System.out.println();
    }
}
