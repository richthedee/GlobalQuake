package globalquake.playground;

import globalquake.core.Settings;
import globalquake.main.Main;
import globalquake.ui.globalquake.GlobalQuakeFrame;
import globalquake.ui.globalquake.GlobalQuakePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GlobalQuakeFramePlayground extends GlobalQuakeFrame {

    public GlobalQuakeFramePlayground() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panel = createGQPanel();

        panel.setPreferredSize(new Dimension(1000, 760));

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        //mainPanel.setPreferredSize(new Dimension(1000, 760));
        mainPanel.add(panel, BorderLayout.CENTER);

        setContentPane(mainPanel);

        setJMenuBar(createJMenuBar());

        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(320, 300));
        setResizable(true);
        setTitle(Main.fullName);
    }

    protected GlobalQuakePanel createGQPanel() {
        return new GlobalQuakePanelPlayground(this);
    }

}