package com.guberan.testanalyzer.gui;

import com.guberan.testanalyzer.model.ProjectAnalysis;
import com.guberan.testanalyzer.service.GitService;
import com.guberan.testanalyzer.service.TestAnalyzer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Main "run" panel of the Swing GUI.
 *
 * <p>Enhancements:
 * <ul>
 *   <li>Uses MigLayout for concise layout</li>
 *   <li>Provides predefined popular OSS repositories for quick testing</li>
 *   <li>Thread‑safe Swing updates</li>
 * </ul>
 */
@Slf4j
public class RunPanel extends JPanel {

    /**
     * Common open‑source repositories useful for conventions analysis.
     */
    private static final Map<String, String> PRESET_REPOS = new LinkedHashMap<>() {{
        put("Spring Framework", "https://github.com/spring-projects/spring-framework");
        put("Spring Boot", "https://github.com/spring-projects/spring-boot");
        put("JUnit 5", "https://github.com/junit-team/junit-framework");
        put("Google Guava", "https://github.com/google/guava");
        put("Apache Commons Lang", "https://github.com/apache/commons-lang");
    }};

    /* ---------- UI ---------- */
    // Persist last used values between runs
    private static final Preferences PREFS = Preferences.userNodeForPackage(RunPanel.class);
    private static final String KEY_LAST_PATH = "lastLocalPath";
    private final JComboBox<String> presetCombo = new JComboBox<>();
    private final JButton urlMenuBtn = new JButton("▼");
    private final JPopupMenu urlMenu = new JPopupMenu();
    private final JTextField urlField = new JTextField();
    private final JTextField pathField = new JTextField(); // /Volumes/Datamag/IdeaProjects/junit-framework"); /Volumes/Datamag/IdeaProjects/spring-boot
    private final JButton browseBtn = new JButton("Browse…");
    private final JButton analyzeBtn = new JButton("Analyze");
    private final JProgressBar progress = new JProgressBar();
    private final JLabel status = new JLabel("Ready.");
    /**
     * Callback invoked when analysis completes successfully.
     */
    @Setter
    private Consumer<ProjectAnalysis> onResults = s -> {
    };

    public RunPanel() {
        initComponents();
        buildLayout();
        wireActions();
    }

    /* ============================================================ */

    private void initComponents() {
        urlField.setToolTipText("Git URL (https://… or git@…) - optional");
        pathField.setToolTipText("Local project root path - optional");

        progress.setStringPainted(true);
        progress.setString("Idle");

        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        setupUrlMenu();

        // restore last used values
        pathField.setText(PREFS.get(KEY_LAST_PATH, ""));
    }

    private void setupUrlMenu() {
        urlMenuBtn.setFocusable(false);
        urlMenuBtn.setMargin(new java.awt.Insets(2, 6, 2, 6));

        PRESET_REPOS.forEach((name, url) -> {
            JMenuItem item = new JMenuItem(name);
            item.addActionListener(e -> urlField.setText(url));
            urlMenu.add(item);
        });

        urlMenuBtn.addActionListener(e -> urlMenu.show(urlMenuBtn, 0, urlMenuBtn.getHeight()));
    }

    private void buildLayout() {
        setLayout(new MigLayout(
                "insets 12, fillx",
                "[][grow,fill][pref!]",
                ""
        ));

        add(new JLabel("Git URL:"), "cell 0 0");
        add(urlField, "cell 1 0");
        add(urlMenuBtn, "cell 2 0");

        add(new JLabel("Local Path:"), "cell 0 2");
        add(pathField, "cell 1 2");
        add(browseBtn, "cell 2 2, width :100:");

        add(new JLabel("Progress:"), "cell 0 3");
        add(progress, "cell 1 3");
        add(analyzeBtn, "cell 2 3, width :100:");

        add(new JLabel("Status:"), "cell 0 4");
        add(status, "cell 1 4 2 1");
    }

    private void wireActions() {
        browseBtn.addActionListener(e -> chooseDir());
        analyzeBtn.addActionListener(e -> runAnalysis());

        presetCombo.addActionListener(e -> {
            String key = (String) presetCombo.getSelectedItem();
            if (PRESET_REPOS.containsKey(key)) {
                urlField.setText(PRESET_REPOS.get(key));
            }
        });
    }

    /* ============================================================ */

    /**
     * Opens a directory chooser and populates the Local Path field.
     */
    private void chooseDir() {
        var chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String selected = chooser.getSelectedFile().toPath().toString();
            pathField.setText(selected);
            PREFS.put(KEY_LAST_PATH, selected);
        }
    }

    /**
     * Runs the analysis asynchronously using SwingWorker.
     */
    private void runAnalysis() {
        setRunningUi("Starting…");

        var url = Optional.ofNullable(urlField.getText()).map(String::trim).filter(s -> !s.isBlank());
        var localPath = Optional.ofNullable(pathField.getText()).map(String::trim).filter(s -> !s.isBlank());

        // persist values
        if (url.isEmpty()) {
            localPath.ifPresent(p -> PREFS.put(KEY_LAST_PATH, p));
        }

        SwingWorker<ProjectAnalysis, ProgressInfo> worker = new SwingWorker<>() {
            @Override
            protected ProjectAnalysis doInBackground() throws Exception {
                Path root;
                if (url.isPresent()) {
                    publish(new ProgressInfo("Cloning repository…", 0, 0));
                    root = new GitService().cloneToTemp(url.get());
                    pathField.setText(root.toString());
                    log.info("localPath: {}", localPath);
                } else if (localPath.isPresent()) {
                    root = Path.of(localPath.get());
                } else {
                    throw new IllegalArgumentException("Provide either Git URL or Local Path.");
                }

                publish(new ProgressInfo("Analyzing project: " + root, 0, 0));
                return new TestAnalyzer().analyze(root, this::publish);
            }

            @Override
            protected void process(java.util.List<ProgressInfo> chunks) {
                if (!chunks.isEmpty()) {
                    ProgressInfo pi = chunks.getLast();

                    // set status if changed
                    if (pi.info != null && !Objects.equals(status.getText(), pi.info)) {
                        status.setText(pi.info);
                    }
                    if (progress.getMaximum() != pi.max) {
                        progress.setMaximum(pi.max);
                        progress.setIndeterminate(pi.max <= 0);
                    }
                    progress.setValue(pi.value);
                }
            }

            @Override
            protected void done() {
                try {
                    onResults.accept(get());
                    status.setText("Done.");
                } catch (Exception ex) {
                    log.error("Analysis failed", ex);
                    JOptionPane.showMessageDialog(RunPanel.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    status.setText("Failed.");
                } finally {
                    setIdleUi("Idle.");
                }
            }
        };

        worker.execute();
    }

    ;

    private void setRunningUi(String statusText) {
        analyzeBtn.setEnabled(false);
        progress.setIndeterminate(true);
        progress.setString("Running…");
        status.setText(statusText);
    }

    private void setIdleUi(String statusText) {
        progress.setIndeterminate(false);
        progress.setString("Idle");
        analyzeBtn.setEnabled(true);
        status.setText(statusText);
    }

    public record ProgressInfo(String info, int max, int value) {
    }
}
