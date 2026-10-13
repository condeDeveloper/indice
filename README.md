# indice

Índices do zero em Java 21: a partir de que seletividade usar o índice lê **mais páginas** do que
não usar.

O juiz é a **varredura completa**. Ela é o juiz certo e não só o conveniente: não usa índice, não
depende de estatística e não depende de como as linhas estão guardadas, então a resposta dela não
pode estar errada por um motivo sutil. Se uma via discorda dela, a errada é a via.

E ele exige **correção, não desempenho**: que a via devolva exatamente as mesmas linhas, sem
faltar, sem repetir e sem inventar. Quantas páginas ela leu não entra no julgamento.

A unidade é a **página lida**, nunca o segundo. Um banco de dados não lê linhas, lê blocos: trazer
um bloco custa o mesmo com uma linha útil dentro ou com cem. E a unidade tem teto conhecido, que é
o número de páginas da tabela, então `1.000` numa tabela de 1.000 páginas é uma frase completa.

## O índice passa a varredura em 1% de seletividade

```
$ mvn exec:java -Dexec.args="seletividade"

  seletividade     linhas via                         paginas       entradas  vezes a varredura
        0.0001         10 varredura completa             1000              0              1.000
        0.0001         10 indice sem ordenar               10             28              0.010
        0.0001         10 indice ordenando                  9             28              0.009
        0.0100       1000 varredura completa             1000              0              1.000
        0.0100       1000 indice sem ordenar             1000           1018              1.000
        0.0100       1000 indice ordenando                630           1018              0.630
        0.1000      10000 varredura completa             1000              0              1.000
        0.1000      10000 indice sem ordenar            10000          10017             10.000
        0.1000      10000 indice ordenando               1000          10017              1.000
        1.0000     100000 varredura completa             1000              0              1.000
        1.0000     100000 indice sem ordenar           100000         100017            100.000
        1.0000     100000 indice ordenando               1000         100017              1.000
```

Cem mil linhas, cem por página, mil páginas. A varredura completa é uma **reta**: mil páginas em
toda seletividade, porque ela lê a tabela e pronto.

O índice sem ordenar é a via que todo mundo imagina: desce, acha a primeira entrada e vai buscando
cada linha onde ela estiver. Ele cobra **uma página por linha devolvida**, não por página distinta,
porque as entradas saem em ordem de chave e a tabela não está nessa ordem.

Duas retas de inclinações diferentes se cruzam. Em 0,01% o índice custa **1% da varredura**. Em 1%
ele **empata**. Em 10% ele lê **dez vezes** mais do que não ter índice nenhum.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `correcao`, `seletividade`, `agrupamento`, `ordenacao` e `pagina`.

## O que as medidas me corrigiram

**Eu achei que ordenar as linhas antes de ir buscá-las salvasse o índice. Não salva. Só salva da
humilhação.**

Ordenar os números de linha antes de ir à tabela é a correção conhecida: como a página de uma linha
é o número dela dividido pelo tamanho da página, ordenar por linha é ordenar por página, e cada
página é pedida uma vez só. É o que um sistema de verdade chama de varredura por mapa de bits.

Funciona: a coluna nunca passa de 1,000. Ela não pode passar, porque não há como pedir mais páginas
distintas do que existem. Eu escrevi isso e parei por aí, satisfeito.

Aí eu pus na mesma tabela a fração da tabela que ela lê:

```
$ mvn exec:java -Dexec.args="ordenacao"

  seletividade     linhas    sem ordenar      ordenando      repetidas fracao da tabela         passos
        0.0001         10             10              9              1           0.0090             40
        0.0010        100            100             93              7           0.0930            700
        0.0050        500            500            384            116           0.3840           4500
        0.0100       1000           1000            630            370           0.6300          10000
        0.0200       2000           2000            877           1123           0.8770          22000
        0.0500       5000           5000            997           4003           0.9970          65000
        0.1000      10000          10000           1000           9000           1.0000         140000
        1.0000     100000         100000           1000          99000           1.0000        1700000
```

As duas pontas dizem a mesma coisa e é uma coisa ruim para o índice.

Em **0,01%**, onde o índice é realmente bom, ordenar economiza **uma página de dez**. As dez linhas
que casam estão em dez páginas diferentes e não há releitura nenhuma a evitar. A correção não faz
nada exatamente onde o índice é útil.

Em **1%**, na virada, a via que ordena já lê **63% da tabela**. Ela ganhou da varredura, tecnicamente.
Ganhou lendo dois terços dela, pagando dez mil passos de ordenação e perdendo a ordem de chave.

Em **5%** ela lê **997 páginas de 1.000**. É uma varredura completa com passos a mais.

Então a frase honesta não é "ordene os ponteiros e o índice volta a servir". É que **a janela em que
um índice não agrupado vale a pena é muito mais estreita do que a virada sugere**, e ordenar mexe no
lado da janela onde já não importa.

### E a virada não depende do tamanho da tabela

Eu também tinha escrito a regra de bolso de sempre, "entre 1% e 5%, depende da tabela". Depende de
uma coisa só, e não é a tabela:

```
$ mvn exec:java -Dexec.args="pagina"

      linhas   por pagina    paginas  virada linhas  virada fracao 1 / por pagina   lido ordenando ali
      100000           10      10000          10001        0.10001        0.10000               0.6511
      100000           25       4000           4001        0.04001        0.04000               0.6418
      100000          100       1000           1001        0.01001        0.01000               0.6310
      100000          400        250            251        0.00251        0.00250               0.6520
      100000         1000        100            101        0.00101        0.00100               0.6200
      400000           10      40000          40001        0.10000        0.10000               0.6505
      400000           25      16000          16001        0.04000        0.04000               0.6379
      400000          100       4000           4001        0.01000        0.01000               0.6363
      400000          400       1000           1001        0.00250        0.00250               0.6330
      400000         1000        400            401        0.00100        0.00100               0.6250
```

A virada é **uma linha a mais do que o número de páginas**, sempre. Em fração, é **um sobre o número
de linhas por página**, e quadruplicar a tabela não mexe nela em nenhuma casa decimal.

Faz sentido depois de medido: o índice sem ordenar lê uma página por linha, a varredura lê
`linhas / por página`, e igualar as duas cancela o número de linhas. O tamanho da tabela sai da
conta inteira. Quem dobra a página dobra o custo da varredura e **divide por dois** a seletividade
em que o índice ainda compensa.

E a última coluna é a mesma piada em todos os dez casos: na virada, seja qual for a página e seja
qual for a tabela, a via que ordena lê perto de **63%**. Isso não foi escolhido nem ajustado. Jogar
tantas linhas quantas há páginas em tantas páginas quantas há acerta `1 - 1/e = 0,632` delas, que é
o mesmo cálculo do problema do aniversário visto do outro lado.

### Uma correção menor, sobre o gerador

O sorteio da casa devolve os 32 bits de cima do estado, e eu tinha escrito a justificativa de
sempre: sem o deslocamento, os bits fracos de baixo estragariam o embaralhamento e a tabela
"aleatória" ficaria meio em ordem.

Medi antes de deixar a frase no arquivo. Não estraga: a consulta de 1% lê **613** páginas com o
gerador cortado nos bits de baixo e **630** com o certo. Fisher e Yates pede um resto por um limite
diferente a cada passo, e isso sozinho dissolve o padrão.

O que quebra de verdade é o sorteio com **limite fixo e potência de dois**, e esse caso existe aqui:
a tabela correlacionada de janela dois pede `ate(2)` o tempo todo, e com os bits de baixo isso
alterna zero, um, zero, um, para sempre. Mil sorteios, mil trocas, acaso nenhum.

Então a razão do deslocamento não é que todo uso quebraria. É que um gerador cujo defeito depende
de qual limite o chamador escolheu é uma armadilha. Os dois testes estão lá, inclusive o que afirma
os 613.

## A ordem das linhas vale 63 vezes

```
$ mvn exec:java -Dexec.args="agrupamento"

  forma              linhas    sem ordenar      ordenando      distintas  vezes a varredura
  agrupada             1000           1000             10             10              0.010
  janela 100           1000           1000             10             10              0.010
  janela 1000          1000           1000             20             20              0.020
  janela 10000         1000           1000            197            197              0.197
  aleatoria            1000           1000            630            630              0.630
```

A mesma consulta, as mesmas chaves, o mesmo índice. Só muda a **ordem em que as linhas estão
guardadas**, que é uma propriedade da tabela e não do índice. Dez páginas contra 630.

E a linha do meio é a interessante. "Janela 100" quer dizer que a linha da chave c está a menos de
cem linhas de c: a tabela não está ordenada, não passa no teste de agrupada, e **custa exatamente o
mesmo** que a agrupada. Com cem linhas por página, uma janela do tamanho da página já faz cada
página guardar uma faixa quase contígua de chaves, e para o índice tanto faz.

Isso não é um caso de laboratório: uma coluna de data de inserção num sistema que só cresce fica
assim sozinha. É por isso que um planejador de verdade guarda a correlação da coluna como
estatística, e não só a cardinalidade.

## O índice agrupado é outro esporte

Numa tabela agrupada as linhas que casam são **contíguas**, então depois de achar a primeira o
índice não serve mais para nada: basta andar na tabela até a chave passar do fim da faixa.

```
  seletividade     linhas via                         paginas       entradas  vezes a varredura
        0.0100       1000 indice agrupado                  11             18              0.011
        0.1000      10000 indice agrupado                 101             17              0.101
        1.0000     100000 indice agrupado                1000             18              1.000
```

Ele lê **17 ou 18 entradas de índice** devolvendo dez linhas ou cem mil, enquanto a via sem ordenar lê
100.017 para devolver cem mil. E ele só empata com a varredura quando devolve a tabela inteira.

Isso não resolve o problema, muda de quem ele é: as linhas só cabem numa ordem, então uma tabela tem
no máximo **um** índice agrupado. Escolher qual coluna agrupa é escolher qual consulta vai ser
rápida. Todas as outras colunas indexadas caem nas vias de cima.

## O que não tem aqui

**Cache de páginas.** A via sem ordenar não guarda a última página lida. Um sistema de verdade tem
um buffer, e com ele a diferença entre as duas vias do índice encolhe nas seletividades baixas, que
é justo onde ela já era pequena. Acima da virada o buffer não salva nada: o conjunto de páginas
pedidas é maior do que qualquer buffer razoável.

**A diferença entre leitura sequencial e aleatória.** Mil páginas em sequência são bem mais rápidas
que mil páginas espalhadas pelo disco, e isso empurra a virada para ainda **mais cedo** do que o
medido. Ou seja, o número daqui é otimista para o índice.

**Uma árvore B de verdade.** O índice aqui é um vetor ordenado com busca binária. A altura e o custo
por nó mudam; quantas páginas da tabela a lista de linhas faz ler, não.

**Atualização.** Nada aqui insere nem apaga, então o custo de manter o índice em dia não aparece em
coluna nenhuma. Ele é real e é mais um peso do lado de não criar o índice.

## Licença

MIT.
