package br.com.conde.indice;

/**
 * A CONSULTA: uma faixa fechada de chaves.
 *
 * <p>Uma faixa e a pergunta certa para este assunto, e nao uma igualdade. Com igualdade de
 * chave unica a resposta tem uma linha so, o indice ganha sempre e nao ha o que medir. E a
 * SELETIVIDADE, que e a fracao de linhas que a consulta devolve, e exatamente o eixo em
 * que tudo aqui vira.
 *
 * <p>Nas tabelas deste repositorio as chaves sao os numeros de zero ate o total de linhas
 * menos um, cada um uma vez so. Isso nao e economia de codigo: e o que permite pedir uma
 * seletividade exata. A faixa de zero a k menos um devolve k linhas, sempre, em qualquer
 * tabela. Sem isso, cada linha das tabelas de medida teria um numero de linhas um pouco
 * diferente e as colunas nao poderiam ser comparadas entre si.
 */
public record Consulta(int de, int ate) {

    public Consulta {
        if (ate < de) throw new IllegalArgumentException("faixa vazia: " + de + " ate " + ate);
    }

    /** A faixa que devolve exatamente tantas linhas numa tabela de chaves de zero a n-1. */
    public static Consulta comTantasLinhas(int linhas) {
        if (linhas < 1) throw new IllegalArgumentException("pelo menos uma linha");

        return new Consulta(0, linhas - 1);
    }

    public boolean casa(int chave) {
        return chave >= de && chave <= ate;
    }

    public int linhasEsperadas() {
        return ate - de + 1;
    }
}
