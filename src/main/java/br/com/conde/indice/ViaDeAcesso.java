package br.com.conde.indice;

/**
 * UMA VIA DE ACESSO: um jeito de achar as linhas que casam com a consulta.
 *
 * <p>Todas devolvem AS MESMAS LINHAS. Essa e a regra do juiz e e o que torna a comparacao
 * uma comparacao: se uma via devolvesse linhas diferentes, ela estaria respondendo outra
 * pergunta, e ler menos paginas nao significaria nada.
 *
 * <p>O que muda entre elas e so a ORDEM em que as linhas saem e quantas paginas foram
 * pedidas pelo caminho. A ordem importa de verdade num sistema de verdade, porque uma
 * consulta que ja pede ordem de chave pode aproveitar a do indice e pular uma ordenacao, e
 * e por isso que o juiz compara copias ordenadas em vez de exigir uma ordem so.
 */
public interface ViaDeAcesso {

    String nome();

    /**
     * As linhas que casam, na ordem em que esta via as produz.
     *
     * <p>O contador precisa ter vindo de prepararPara com o numero de paginas desta tabela.
     */
    int[] buscar(Consulta consulta, Contador contador);
}
