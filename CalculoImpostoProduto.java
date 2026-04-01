import java.awt.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// =============================================================================
// REVISÃO DE CÓDIGO — DESENVOLVEDOR SÊNIOR
// =============================================================================
// OBJETIVO PEDAGÓGICO:
//   Este arquivo demonstra boas práticas para uma aplicação Swing de cálculo
//   de impostos. Cada decisão de refatoração está documentada com o "porquê",
//   para que o desenvolvedor júnior compreenda não só o "o quê", mas a razão
//   por trás de cada escolha técnica.
//
// PRINCIPAIS MELHORIAS APLICADAS:
//   1. BigDecimal para valores monetários (precisão exata).
//   2. Separação de responsabilidades: classe interna "CalculadoraImpostos"
//      concentra toda a lógica de negócio, enquanto a JFrame cuida apenas da UI.
//   3. Formatação monetária com NumberFormat/Locale (padrão pt-BR).
//   4. Normalização de entrada (vírgula → ponto) centralizada.
//   5. Constantes nomeadas substituem "magic numbers".
//   6. Métodos com responsabilidade única (princípio SRP do SOLID).
// =============================================================================

public class CalculoImpostoProduto extends JFrame {

    // -------------------------------------------------------------------------
    // CONSTANTES DE LAYOUT
    // -------------------------------------------------------------------------
    // POR QUÊ usar constantes em vez de números literais espalhados?
    // Se precisarmos mudar a largura do campo, alteramos UM lugar, não dez.
    // Isso é o princípio DRY (Don't Repeat Yourself).
    // -------------------------------------------------------------------------
    private static final int FIELD_WIDTH  = 160;
    private static final int FIELD_HEIGHT = 26;

    // -------------------------------------------------------------------------
    // CONSTANTE DE MARGEM DE LUCRO
    // -------------------------------------------------------------------------
    // POR QUÊ extrair "2.0" para uma constante chamada MULTIPLICADOR_LUCRO?
    // O número 2.0 solto no código é chamado de "magic number": ninguém sabe,
    // ao ler, que ele representa "100% de margem". Com o nome descritivo,
    // a intenção fica clara e a alteração futura é trivial.
    // -------------------------------------------------------------------------
    private static final BigDecimal MULTIPLICADOR_LUCRO = new BigDecimal("2.0");

    // -------------------------------------------------------------------------
    // COMPONENTES DE ENTRADA
    // -------------------------------------------------------------------------
    private JTextField valorField, ipiField, icmsField, freteField, dolarField;
    private JCheckBox  sultanCheckBox;

    // -------------------------------------------------------------------------
    // COMPONENTES DE SAÍDA (labels de resultado)
    // -------------------------------------------------------------------------
    private JLabel valorCompraLabel, valorCustoLabel, valorFinalLabel;
    private JLabel icmsResultLabel, ipiResultLabel, freteResultLabel, dolarResultLabel;

    // -------------------------------------------------------------------------
    // FORMATADOR MONETÁRIO BRASILEIRO
    // -------------------------------------------------------------------------
    // POR QUÊ usar NumberFormat com Locale.forLanguageTag("pt-BR")?
    //   - Formata automaticamente como "R$ 1.198,00" (ponto para milhar,
    //     vírgula para decimal), seguindo a norma ABNT e a expectativa do
    //     usuário brasileiro.
    //   - Centralizar o formatador como campo evita instanciá-lo toda vez que
    //     um cálculo ocorre (economiza memória e CPU em uso intensivo).
    // POR QUÊ NÃO usar String.format("R$ %.2f", valor)?
    //   - String.format usa o Locale padrão da JVM, que pode variar entre
    //     máquinas e produzir saídas inconsistentes. Locale explícito garante
    //     resultado previsível em qualquer ambiente.
    // -------------------------------------------------------------------------
    private static final NumberFormat FORMATO_MOEDA =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    // =========================================================================
    // CLASSE INTERNA: LÓGICA DE NEGÓCIO (Separação de Responsabilidades)
    // =========================================================================
    // POR QUÊ criar uma classe interna estática para a lógica de cálculo?
    //
    // Princípio da Responsabilidade Única (SRP — Single Responsibility Principle):
    //   - A JFrame deve ser responsável apenas por exibir e capturar dados.
    //   - A lógica de negócio (fórmulas, regras) deve estar em um local isolado.
    //
    // Vantagens práticas:
    //   a) Testabilidade: podemos testar "CalculadoraImpostos" sem abrir janela.
    //   b) Manutenção: mudar uma fórmula não exige mexer no código de UI.
    //   c) Legibilidade: quem lê a JFrame vê apenas UI; quem lê a calculadora
    //      vê apenas regras de negócio.
    //
    // POR QUÊ "static"? Porque ela não precisa acessar membros de instância da
    // JFrame, tornando-a independente e mais leve.
    // =========================================================================
    static class CalculadoraImpostos {

        // Campos imutáveis: recebem valor no construtor e nunca mudam.
        // POR QUÊ imutabilidade? Objetos imutáveis são mais seguros em
        // ambientes concorrentes e evitam estados inconsistentes.
        final BigDecimal valorCompra;
        final BigDecimal valorIpi;
        final BigDecimal valorIcms;
        final BigDecimal valorFrete;
        final BigDecimal valorCusto;
        final BigDecimal valorFinal;
        final BigDecimal percentualIpi;
        final BigDecimal percentualIcms;
        final BigDecimal percentualFrete;
        final BigDecimal taxaDolar;

        // -----------------------------------------------------------------
        // CONSTRUTOR: recebe todos os inputs como BigDecimal e aplica as
        // regras de negócio definidas no contexto do sistema.
        // -----------------------------------------------------------------
        CalculadoraImpostos(BigDecimal valorProduto,
                            BigDecimal taxaDolar,
                            BigDecimal ipiPct,
                            BigDecimal icmsPct,
                            BigDecimal fretePct) {

            this.taxaDolar       = taxaDolar;
            this.percentualIpi   = ipiPct;
            this.percentualIcms  = icmsPct;
            this.percentualFrete = fretePct;

            // -----------------------------------------------------------------
            // REGRA: Valor de Compra (BASE DO SISTEMA)
            // Se dólar for zero ou não informado, usa o valor do produto
            // diretamente. Caso contrário, multiplica.
            //
            // POR QUÊ BigDecimal.ZERO.compareTo() em vez de "== 0"?
            //   BigDecimal é um objeto. Comparar com "==" compara referências,
            //   não valores. O método compareTo() é a forma correta de
            //   comparar valores numéricos entre instâncias de BigDecimal.
            // -----------------------------------------------------------------
            if (taxaDolar.compareTo(BigDecimal.ZERO) == 0) {
                this.valorCompra = valorProduto;
            } else {
                // MathContext.DECIMAL128 garante alta precisão na multiplicação,
                // evitando overflow ou perda de casas decimais significativas.
                this.valorCompra = valorProduto.multiply(taxaDolar, MathContext.DECIMAL128);
            }

            // -----------------------------------------------------------------
            // REGRA: Impostos são calculados sempre sobre o Valor de Compra.
            // Fórmula: imposto = valorCompra * (percentual / 100)
            //
            // POR QUÊ dividir por "100" como BigDecimal e não como int?
            //   BigDecimal.valueOf(100) mantém a precisão aritmética.
            //   Se usássemos int ou double intermediário, poderíamos introduzir
            //   erros de ponto flutuante (ex: 0.1 + 0.2 != 0.3 em double).
            // -----------------------------------------------------------------
            BigDecimal cem = BigDecimal.valueOf(100);
            this.valorIpi   = valorCompra.multiply(ipiPct.divide(cem,   MathContext.DECIMAL128));
            this.valorIcms  = valorCompra.multiply(icmsPct.divide(cem,  MathContext.DECIMAL128));
            this.valorFrete = valorCompra.multiply(fretePct.divide(cem, MathContext.DECIMAL128));

            // -----------------------------------------------------------------
            // REGRA: Valor de Custo = soma de compra + todos os impostos.
            // -----------------------------------------------------------------
            this.valorCusto = valorCompra
                    .add(valorIpi)
                    .add(valorIcms)
                    .add(valorFrete);

            // -----------------------------------------------------------------
            // REGRA: Valor Final = Custo * 2 (margem fixa de 100%).
            // A constante MULTIPLICADOR_LUCRO torna a intenção explícita.
            // -----------------------------------------------------------------
            this.valorFinal = valorCusto.multiply(MULTIPLICADOR_LUCRO, MathContext.DECIMAL128);
        }
    }

    // =========================================================================
    // CONSTRUTOR DA JANELA
    // =========================================================================
    public CalculoImpostoProduto() {
        setTitle("Calculo de Impostos do Produto");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Paleta de cores e fontes definidas localmente.
        // Em projetos maiores, estas seriam movidas para uma classe de tema/tema.
        Color backgroundColor = new Color(245, 245, 250);
        Color labelColor      = new Color(60, 60, 60);
        Color buttonColor     = new Color(70, 130, 180);
        Font  labelFont       = new Font("Segoe UI", Font.PLAIN, 14);
        Font  resultFont      = new Font("Segoe UI", Font.PLAIN, 14);
        Font  buttonFont      = new Font("Segoe UI", Font.BOLD, 13);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(backgroundColor);
        panel.setBorder(new EmptyBorder(18, 24, 18, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.NONE;

        // Criação dos campos de texto com largura uniforme.
        valorField = criarCampo();
        dolarField = criarCampo();
        ipiField   = criarCampo();
        icmsField  = criarCampo();
        freteField = criarCampo();

        // Montagem dos campos rotulados na grade.
        int row = 0;
        adicionarCampoRotulado(panel, "Valor do Produto:", valorField, gbc, row++, labelFont, labelColor);
        adicionarCampoRotulado(panel, "Dolar:",            dolarField, gbc, row++, labelFont, labelColor);
        adicionarCampoRotulado(panel, "IPI (%):",          ipiField,   gbc, row++, labelFont, labelColor);
        adicionarCampoRotulado(panel, "ICMS (%):",         icmsField,  gbc, row++, labelFont, labelColor);
        row = adicionarLinhaFrete(panel, gbc, row, labelFont, labelColor);

        // -----------------------------------------------------------------
        // BOTÕES: cada botão tem uma única ação (SRP aplicado a eventos).
        // As lambdas (e -> ...) são a forma moderna de implementar
        // ActionListener sem criar classes anônimas verbosas.
        // -----------------------------------------------------------------
        JButton calcularButton = criarBotao("Calcular", buttonColor,            Color.WHITE, buttonFont);
        JButton limparButton   = criarBotao("Limpar",   Color.DARK_GRAY,        Color.WHITE, buttonFont);
        JButton sairButton     = criarBotao("Sair",     new Color(220, 20, 60), Color.WHITE, buttonFont);

        calcularButton.addActionListener(e -> acaoCalcular());
        limparButton  .addActionListener(e -> limparCampos());
        sairButton    .addActionListener(e -> System.exit(0));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.add(calcularButton);
        buttonPanel.add(limparButton);
        buttonPanel.add(sairButton);

        gbc.gridx     = 0;
        gbc.gridy     = row++;
        gbc.gridwidth = 2;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = new Insets(10, 5, 6, 5);
        panel.add(buttonPanel, gbc);

        // Separador visual entre inputs e resultados.
        gbc.gridy  = row++;
        gbc.insets = new Insets(2, 5, 8, 5);
        panel.add(new JSeparator(), gbc);

        // -----------------------------------------------------------------
        // LABELS DE RESULTADO
        // Inicializados apenas com o rótulo (sem valor) para não confundir
        // o usuário antes do primeiro cálculo.
        // -----------------------------------------------------------------
        gbc.fill      = GridBagConstraints.NONE;
        gbc.gridwidth = 2;
        gbc.insets    = new Insets(3, 5, 3, 5);

        valorCompraLabel = criarLabelResultado("Valor de Compra:", resultFont);
        valorCustoLabel  = criarLabelResultado("Valor Custo:",     resultFont);
        valorFinalLabel  = criarLabelResultado("Valor Final:",     resultFont);
        icmsResultLabel  = criarLabelResultado("ICMS:",            resultFont);
        ipiResultLabel   = criarLabelResultado("IPI:",             resultFont);
        freteResultLabel = criarLabelResultado("Frete:",           resultFont);
        dolarResultLabel = criarLabelResultado("Dolar:",           resultFont);

        gbc.gridy = row++; panel.add(valorCompraLabel, gbc);
        gbc.gridy = row++; panel.add(valorCustoLabel,  gbc);
        gbc.gridy = row++; panel.add(valorFinalLabel,  gbc);
        gbc.gridy = row++; panel.add(icmsResultLabel,  gbc);
        gbc.gridy = row++; panel.add(ipiResultLabel,   gbc);
        gbc.gridy = row++; panel.add(freteResultLabel, gbc);
        gbc.gridy = row;   panel.add(dolarResultLabel, gbc);

        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // =========================================================================
    // LINHA DE FRETE COM CHECKBOX "SULTAN"
    // =========================================================================
    // Extraído em método próprio por ser mais complexo que os outros campos,
    // mantendo o construtor limpo e legível.
    // =========================================================================
    private int adicionarLinhaFrete(JPanel panel, GridBagConstraints gbc, int row,
                                    Font labelFont, Color labelColor) {
        gbc.gridwidth = 1;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.gridx     = 0;
        gbc.gridy     = row;
        gbc.insets    = new Insets(5, 5, 5, 5);

        JLabel label = new JLabel("Frete (%):");
        label.setFont(labelFont);
        label.setForeground(labelColor);
        panel.add(label, gbc);

        // Container horizontal: campo + checkbox Sultan lado a lado.
        JPanel fretePanel = new JPanel();
        fretePanel.setLayout(new BoxLayout(fretePanel, BoxLayout.X_AXIS));
        fretePanel.setOpaque(false);

        // Força dimensão fixa idêntica aos demais campos para alinhamento uniforme.
        freteField.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        freteField.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        freteField.setMinimumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        fretePanel.add(freteField);
        fretePanel.add(Box.createHorizontalStrut(8));

        // -----------------------------------------------------------------
        // CHECKBOX SULTAN
        // Quando marcado: define frete fixo em 5% e bloqueia o campo.
        // Quando desmarcado: limpa o campo e o reabilita.
        //
        // POR QUÊ usar "5" (String) em vez de 5 (int)?
        //   freteField.setText() espera String. Passamos "5" diretamente para
        //   evitar a conversão desnecessária Integer.toString(5).
        // -----------------------------------------------------------------
        sultanCheckBox = new JCheckBox("Sultan");
        sultanCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sultanCheckBox.setOpaque(false);
        sultanCheckBox.addActionListener(e -> {
            boolean ativo = sultanCheckBox.isSelected();
            freteField.setEnabled(!ativo);
            freteField.setText(ativo ? "5" : "");
        });
        fretePanel.add(sultanCheckBox);

        gbc.gridx = 1;
        panel.add(fretePanel, gbc);
        return row + 1;
    }

    // =========================================================================
    // NORMALIZAÇÃO DE ENTRADA
    // =========================================================================
    // POR QUÊ centralizar a normalização em métodos dedicados?
    //   Se o usuário digitar "10,50", a conversão para "10.50" ocorre aqui,
    //   ANTES de qualquer cálculo. Assim, a lógica de negócio nunca recebe
    //   uma String com vírgula — ela sempre trabalha com ponto decimal.
    //   Isso evita NumberFormatException e torna o sistema mais robusto.
    // =========================================================================

    /**
     * Converte a String do campo em BigDecimal, aceitando vírgula como separador.
     * Lança NumberFormatException se o texto for inválido (tratado em acaoCalcular).
     */
    private BigDecimal parsearNumero(String texto) {
        // replace(",", ".") normaliza entradas brasileiras como "1.500,75" → "1.500.75"
        // ATENÇÃO: se o usuário usar ponto como separador de milhar (ex: "1.500"),
        // isso pode gerar erro. Uma melhoria futura seria usar um InputVerifier dedicado.
        return new BigDecimal(texto.trim().replace(",", "."));
    }

    /**
     * Como parsearNumero, mas retorna BigDecimal.ZERO se o campo estiver vazio ou for "0".
     * Usado para campos opcionais (ex: Dólar).
     */
    private BigDecimal parsearNumeroOuZero(String texto) {
        String limpo = texto.trim().replace(",", ".");
        if (limpo.isEmpty() || limpo.equals("0")) return BigDecimal.ZERO;
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    // =========================================================================
    // AÇÃO DO BOTÃO CALCULAR
    // =========================================================================
    // Este método é o "orquestrador": coleta entradas, delega o cálculo à
    // CalculadoraImpostos e exibe os resultados. Ele NÃO realiza matemática —
    // isso seria mistura de responsabilidades.
    // =========================================================================
    private void acaoCalcular() {
        try {
            // -----------------------------------------------------------------
            // LEITURA E NORMALIZAÇÃO DAS ENTRADAS
            // Todos os valores são convertidos para BigDecimal aqui, antes de
            // qualquer operação matemática.
            // -----------------------------------------------------------------
            BigDecimal valorOriginal = parsearNumero(valorField.getText());
            BigDecimal ipi           = parsearNumero(ipiField.getText());
            BigDecimal icms          = parsearNumero(icmsField.getText());
            BigDecimal frete         = parsearNumero(freteField.getText());
            BigDecimal dolar         = parsearNumeroOuZero(dolarField.getText());

            // -----------------------------------------------------------------
            // DELEGAÇÃO DO CÁLCULO
            // A JFrame não conhece as fórmulas — ela apenas instancia a
            // calculadora e consome os resultados. Separação limpa.
            // -----------------------------------------------------------------
            CalculadoraImpostos calc = new CalculadoraImpostos(valorOriginal, dolar, ipi, icms, frete);

            // -----------------------------------------------------------------
            // FORMATAÇÃO E EXIBIÇÃO DOS RESULTADOS
            // POR QUÊ FORMATO_MOEDA.format() e não String.format("%.2f")?
            //   - FORMATO_MOEDA respeita o Locale pt-BR: usa vírgula decimal
            //     e ponto para milhar, exibe o símbolo "R$" automaticamente.
            //   - String.format("%.2f") usa o Locale padrão da JVM e pode
            //     produzir "." decimal em sistemas configurados em inglês.
            //
            // IMPORTANTE: a formatação acontece SOMENTE aqui, na camada de UI.
            // Os BigDecimals dentro de CalculadoraImpostos nunca são alterados.
            // -----------------------------------------------------------------
            valorCompraLabel.setText("Valor de Compra: " + FORMATO_MOEDA.format(calc.valorCompra));
            valorCustoLabel .setText("Valor Custo: "     + FORMATO_MOEDA.format(calc.valorCusto));
            valorFinalLabel .setText("Valor Final: "     + FORMATO_MOEDA.format(calc.valorFinal));

            // Percentuais exibidos com duas casas, também usando setScale apenas
            // para exibição — o BigDecimal original permanece intacto.
            icmsResultLabel .setText(String.format(Locale.forLanguageTag("pt-BR"),
                    "ICMS: %.2f%%", calc.percentualIcms));
            ipiResultLabel  .setText(String.format(Locale.forLanguageTag("pt-BR"),
                    "IPI: %.2f%%",  calc.percentualIpi));
            freteResultLabel.setText(String.format(Locale.forLanguageTag("pt-BR"),
                    "Frete: %.2f%%", calc.percentualFrete));

            // Dólar: exibe "0,00" se não informado, ou o valor real.
            BigDecimal dolarExibido = dolar.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO : dolar;
            dolarResultLabel.setText(String.format(Locale.forLanguageTag("pt-BR"),
                    "Dolar: %.2f", dolarExibido));

            // Reajusta o tamanho da janela para acomodar os novos textos.
            pack();

        } catch (NumberFormatException ex) {
            // -----------------------------------------------------------------
            // TRATAMENTO DE ERRO DE ENTRADA
            // NumberFormatException ocorre quando o usuário digita texto
            // não numérico (ex: "abc") em um campo esperado numérico.
            // A mensagem orienta o usuário sem expor detalhes técnicos.
            // -----------------------------------------------------------------
            JOptionPane.showMessageDialog(this,
                "Insira valores validos.\nUse ponto ou virgula como separador decimal.",
                "Erro de entrada", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // AÇÃO DO BOTÃO LIMPAR
    // =========================================================================
    // Reseta todos os campos de entrada e os labels de saída ao estado inicial.
    // Também garante que o checkbox Sultan e o campo de frete sejam resetados
    // de forma consistente (ativo/inativo conforme o estado do checkbox).
    // =========================================================================
    private void limparCampos() {
        valorField.setText("");
        dolarField.setText("");
        ipiField.setText("");
        icmsField.setText("");

        // Reseta o checkbox e reabilita manualmente o campo de frete,
        // pois o listener do checkbox só dispara em ação do usuário, não em setText.
        sultanCheckBox.setSelected(false);
        freteField.setEnabled(true);
        freteField.setText("");

        // Reseta os labels para o estado inicial (apenas o rótulo, sem valor).
        valorCompraLabel.setText("Valor de Compra:");
        valorCustoLabel .setText("Valor Custo:");
        valorFinalLabel .setText("Valor Final:");
        icmsResultLabel .setText("ICMS:");
        ipiResultLabel  .setText("IPI:");
        freteResultLabel.setText("Frete:");
        dolarResultLabel.setText("Dolar:");
    }

    // =========================================================================
    // HELPERS DE UI
    // =========================================================================
    // Métodos auxiliares que criam componentes padronizados.
    // POR QUÊ extrair em métodos em vez de repetir o código?
    //   DRY (Don't Repeat Yourself): se mudarmos a fonte padrão dos campos,
    //   mudamos em um único lugar.
    // =========================================================================

    /** Cria um JTextField com fonte e dimensão padrão do sistema. */
    private JTextField criarCampo() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        return field;
    }

    /** Adiciona um par (JLabel, JTextField) em uma linha da grade GridBag. */
    private void adicionarCampoRotulado(JPanel panel, String textoLabel, JTextField field,
                                         GridBagConstraints gbc, int row,
                                         Font labelFont, Color labelColor) {
        gbc.gridwidth = 1;
        gbc.fill      = GridBagConstraints.NONE;
        gbc.gridx     = 0;
        gbc.gridy     = row;
        gbc.insets    = new Insets(5, 5, 5, 5);

        JLabel label = new JLabel(textoLabel);
        label.setFont(labelFont);
        label.setForeground(labelColor);
        panel.add(label, gbc);

        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    /** Cria um JButton com cores e fonte personalizadas. */
    private JButton criarBotao(String texto, Color bg, Color fg, Font font) {
        JButton b = new JButton(texto);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(font);
        b.setFocusPainted(false);
        return b;
    }

    /** Cria um JLabel de resultado com fonte padrão e texto inicial. */
    private JLabel criarLabelResultado(String texto, Font font) {
        JLabel label = new JLabel(texto);
        label.setFont(font);
        return label;
    }

    // =========================================================================
    // PONTO DE ENTRADA DA APLICAÇÃO
    // =========================================================================
    // POR QUÊ usar SwingUtilities.invokeLater()?
    //   Todas as operações de UI no Swing DEVEM acontecer na EDT (Event Dispatch
    //   Thread). SwingUtilities.invokeLater() garante que a janela seja criada
    //   nessa thread, evitando condições de corrida e comportamento imprevisível.
    //   Esta é uma regra fundamental do Swing, não uma sugestão.
    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(CalculoImpostoProduto::new);
    }
}
