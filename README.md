# Calculadora de Impostos do Produto

Uma aplicação desktop desenvolvida em Java que calcula o custo real e o preço final de um produto, levando em conta impostos como IPI e ICMS, frete e variação cambial (dólar).

---

## O que o sistema faz

Quando você compra um produto, o preço que paga raramente é o único custo envolvido. Há impostos, frete e, dependendo do caso, conversão de moeda. Esta calculadora reúne todos esses fatores em um lugar só e entrega três valores fundamentais: quanto você pagou pelo produto, quanto ele realmente custou para a sua empresa e por quanto você deveria vendê-lo.

---

## Funcionalidades

- Calculo do valor de compra com ou sem conversao cambial
- Aplicacao de IPI, ICMS e Frete sobre o valor de compra
- Calculo automatico do valor de custo e do preco final
- Checkbox "Sultan" que define o frete fixo em 5% automaticamente
- Aceita valores digitados com virgula ou ponto como separador decimal
- Exibe todos os resultados de forma clara: Valor de Compra, Valor de Custo, Valor Final, percentuais de ICMS, IPI, Frete e taxa do Dolar

---

## Logica de Calculo

O sistema segue um fluxo sequencial e previsivel:

**1. Valor de Compra**

Este e o ponto de partida de tudo. Se o campo Dolar estiver vazio ou zerado, o valor de compra e simplesmente o valor do produto. Se o Dolar for informado, o produto e convertido:

```
valorCompra = valorProduto * dolar
```

**2. Impostos**

IPI, ICMS e Frete sao sempre calculados sobre o valor de compra, nunca sobre o valor original do produto:

```
valorIPI   = valorCompra * (IPI / 100)
valorICMS  = valorCompra * (ICMS / 100)
valorFrete = valorCompra * (Frete / 100)
```

**3. Valor de Custo**

Soma de tudo que foi gasto para ter o produto disponivel:

```
valorCusto = valorCompra + valorIPI + valorICMS + valorFrete
```

**4. Valor Final**

Preco de venda sugerido com margem de 100% sobre o custo:

```
valorFinal = valorCusto * 2
```

---

## Como usar

1. Abra o executavel `CalculoImpostoProduto.exe` dentro da pasta `output`
2. Preencha os campos com os valores do produto
3. Clique em **Calcular**
4. Os resultados aparecem logo abaixo dos botoes

Para limpar tudo e comecar um novo calculo, clique em **Limpar**.

### Exemplo pratico

Situacao: um produto importado por 100 dolares, com Dolar a 5,80, IPI de 10%, ICMS de 12% e Frete de 5%.

| Campo | Valor |
|---|---|
| Valor do Produto | 100 |
| Dolar | 5,80 |
| IPI | 10 |
| ICMS | 12 |
| Frete | 5 |

Resultados:

| Item | Calculo | Valor |
|---|---|---|
| Valor de Compra | 100 x 5,80 | R$ 580,00 |
| IPI | 10% de 580 | R$ 58,00 |
| ICMS | 12% de 580 | R$ 69,60 |
| Frete | 5% de 580 | R$ 29,00 |
| **Valor de Custo** | 580 + 58 + 69,60 + 29 | **R$ 736,60** |
| **Valor Final** | 736,60 x 2 | **R$ 1.473,20** |

---

## Problemas comuns

| Situacao | Solucao |
|---|---|
| O botao "Calcular" nao faz nada | Verifique se os campos obrigatorios estao preenchidos: Valor do Produto, IPI, ICMS e Frete |
| Apareceu uma mensagem de erro ao calcular | Algum campo foi preenchido com texto ou caractere invalido — use apenas numeros, virgula ou ponto |
| O campo de frete esta bloqueado | O checkbox "Sultan" esta marcado; desmarque-o para digitar o frete manualmente |

---

## Estrutura do Projeto

```
Calculadora de Impostos/
├── CalculoImpostoProduto.java       # Codigo-fonte principal
├── CalculoImpostoProduto.class      # Bytecode compilado
├── CalculoImpostoProduto.jar        # Arquivo executavel Java
├── MANIFEST.MF                      # Configuracao do JAR
├── banana.ico                       # Icone da aplicacao
├── output/                          # Executavel final (.exe)
└── pom.xml                          # Configuracao do projeto (Maven)
```

---

## Requisitos Tecnicos

- Java 21 ou superior (apenas para compilar ou rodar o `.jar`)
- O executavel na pasta `output` ja inclui o Java embutido e nao precisa de instalacao adicional

---

## Tecnologias utilizadas

- Java 21
- Swing (interface grafica)
- BigDecimal (precisao em calculos monetarios)
- jpackage (empacotamento do executavel)
