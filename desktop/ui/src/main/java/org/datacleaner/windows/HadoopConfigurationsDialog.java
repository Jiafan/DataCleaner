package org.datacleaner.windows;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import org.apache.metamodel.util.FileResource;
import org.apache.metamodel.util.Resource;
import org.datacleaner.bootstrap.DCWindowContext;
import org.datacleaner.bootstrap.WindowContext;
import org.datacleaner.configuration.DataCleanerConfiguration;
import org.datacleaner.configuration.DatastoreXmlExternalizer;
import org.datacleaner.configuration.ServerInformation;
import org.datacleaner.configuration.ServerInformationCatalog;
import org.datacleaner.configuration.ServerInformationCatalogImpl;
import org.datacleaner.panels.DCBannerPanel;
import org.datacleaner.panels.DCPanel;
import org.datacleaner.panels.HadoopDirectConnectionPanel;
import org.datacleaner.server.DirectConnectionHadoopClusterInformation;
import org.datacleaner.server.DirectoryBasedHadoopClusterInformation;
import org.datacleaner.server.EnvironmentBasedHadoopClusterInformation;
import org.datacleaner.user.MutableServerInformationCatalog;
import org.datacleaner.user.ServerInformationChangeListener;
import org.datacleaner.user.UserPreferences;
import org.datacleaner.user.UserPreferencesImpl;
import org.datacleaner.util.HadoopResource;
import org.datacleaner.util.IconUtils;
import org.datacleaner.util.ImageManager;
import org.datacleaner.util.LookAndFeelManager;
import org.datacleaner.util.WidgetFactory;
import org.datacleaner.util.WidgetUtils;
import org.datacleaner.widgets.Alignment;
import org.datacleaner.widgets.DCLabel;
import org.datacleaner.widgets.DCScrollBarUI;
import org.datacleaner.widgets.FileSelectionListener;
import org.datacleaner.widgets.FilenameTextField;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.JXTextField;
import org.jfree.ui.tabbedui.VerticalLayout;

public class HadoopConfigurationsDialog extends AbstractWindow  implements ServerInformationChangeListener{

   
    
    public class DirectoryConfigurationPanel extends DCPanel {

        public class DirectoryPanel extends DCPanel {

            private static final long serialVersionUID = 1L;

            private final JLabel _label;
            private final FilenameTextField _directoryTextField;
            private File _directory;
            private JButton _removeButton;
            private JPanel _parent;
            private final int _pathNumber;

            public DirectoryPanel(int pathNumber, File directory, JPanel parent) {
                _pathNumber = pathNumber;
                _parent = parent;
                _directory = directory;
                _label = DCLabel.dark("Path " + pathNumber + ":");
                if (directory == null) {
                    _directoryTextField = new FilenameTextField(_userPreferences.getConfiguredFileDirectory(), true);
                } else {
                    _directoryTextField = new FilenameTextField(directory, true);
                }
                _directoryTextField.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                _directoryTextField.addFileSelectionListener(new FileSelectionListener() {

                    @Override
                    public void onSelected(FilenameTextField filenameTextField, File file) {
                        _directory = file;

                        // TODO:Move this in other place
                        // final List<String> paths =
                        // Arrays.asList(server.getDirectories());
                        // paths.add(_directory.getPath());
                        // ServerInformation serverInformation = new
                        // DirectoryBasedHadoopClusterInformation(server.getName(),
                        // server.getDescription(), paths.toArray(new
                        // String[paths.size()]));
                        // _serverInformationCatalog.addServerInformation(serverInformation);
                    }
                });

                if (_directory != null) {
                    _directoryTextField.setFile(_directory);
                }
                _removeButton = WidgetFactory.createDefaultButton("", IconUtils.ACTION_DELETE);
            }

            public DCPanel getPanel() {
                final DCPanel dcPanel = new DCPanel();
                dcPanel.setLayout(new HorizontalLayout(10));
                dcPanel.add(_label);
                dcPanel.add(_directoryTextField);
                dcPanel.add(_removeButton);
                dcPanel.setBorder(WidgetUtils.BORDER_TOP_PADDING);

                _removeButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        _directoryListPanels.remove(_pathNumber);
                        _parent.remove(dcPanel);
                    }
                });
                return dcPanel;
            }

            public File getDirectory() {
                return _directory;
            }
        }

        
        private static final long serialVersionUID = 1L;
        private JPanel _mainPanel;
        private final JXTextField _name;
        private List<DirectoryPanel> _directoryListPanels;
        private final DirectoryBasedHadoopClusterInformation _server;
        private final JPanel _parent;

        public DirectoryConfigurationPanel(final DirectoryBasedHadoopClusterInformation server, JPanel parent) {
            _server = server;
            _name = WidgetFactory.createTextField(server.getName());
            _directoryListPanels = getDirectoriesListPanel(_mainPanel);
            _parent = parent;
            _mainPanel = createPanel();
        }

        public JPanel createPanel() {
            final DCPanel mainPanel = new DCPanel().setTitledBorder("Configuration -" + _server.getName());
            mainPanel.setLayout(new GridBagLayout());

            final JPanel listPanel = new DCPanel();
            listPanel.setLayout(new VerticalLayout());
            for (int i = 0; i < _directoryListPanels.size(); i++) {
                listPanel.add(_directoryListPanels.get(i).getPanel());
            }
            listPanel.addContainerListener(new ContainerListener() {

                @Override
                public void componentRemoved(ContainerEvent e) {
                    mainPanel.revalidate();
                    mainPanel.repaint();
                }

                @Override
                public void componentAdded(ContainerEvent e) {
                    mainPanel.revalidate();
                    mainPanel.repaint();

                }
            });

            final JButton addPath = WidgetFactory.createDefaultButton("Add");
            addPath.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    final DirectoryPanel newPathPanel = new DirectoryPanel(_directoryListPanels.size(), null,
                            listPanel);
                    _directoryListPanels.add(newPathPanel);
                    listPanel.add(newPathPanel.getPanel());

                }
            });

            WidgetUtils.addToGridBag(DCLabel.dark("Name:"), mainPanel, 0, 0, 1, 1, GridBagConstraints.WEST, 4, 0, 0); 
            WidgetUtils.addToGridBag(_name, mainPanel, 1, 0, 1, 1, GridBagConstraints.WEST, 4, 0, 0); 
            WidgetUtils.addToGridBag(listPanel, mainPanel, 0, 1, 4, 1, GridBagConstraints.WEST, 4, 0, 0);
            WidgetUtils.addToGridBag(addPath, mainPanel, 3, 2, 0, 0, GridBagConstraints.SOUTHEAST, 4, 0, 0);
            
            return mainPanel; 
        }
        
        
        public JPanel getMainPanel() {
            return _mainPanel;
        }
        
        private List<DirectoryPanel> getDirectoriesListPanel(JPanel parent) {
         _directoryListPanels = new ArrayList<>(); 
          if (_server != null) {
            final String[] directories = _server.getDirectories();
                if (directories != null) {
                    for (int j = 0; j < directories.length; j++) {
                        final DirectoryPanel directoryPanel = new DirectoryPanel(j, new File(directories[j]), parent);
                        _directoryListPanels.add(directoryPanel);
                    }
                }else{
                    _directoryListPanels.add(new DirectoryPanel(0, null, parent)); 
                }
          }
            return _directoryListPanels;
        }
        
        
       
        
    }
    private static final long serialVersionUID = 1L;

    private final ImageManager imageManager = ImageManager.get();
    private final UserPreferences _userPreferences;
    private final DataCleanerConfiguration _configuration;
    private final MutableServerInformationCatalog _serverInformationCatalog;
    private final DCPanel _defaultConfigurationPanel;
    private final JPanel _directoriesConfigurationsPanel;
    private final DCPanel _directConnectionsConfigurationsPanel;
    private final WindowContext _windowContext; 
    private final List<DirectoryConfigurationPanel> _directoriesConfigurationsListPanels; 
    private final List<HadoopDirectConnectionPanel> _directConnectionsListPanels; 

    // private final JXTextField _columnTextField;

    @Inject
    public HadoopConfigurationsDialog(WindowContext windowContext, DataCleanerConfiguration configuration,
            UserPreferences userPreferences, final MutableServerInformationCatalog serverInformationCatalog) {
        super(windowContext);

        _windowContext = windowContext; 
        _userPreferences = userPreferences;
        _configuration = configuration;
        _serverInformationCatalog = serverInformationCatalog;
        _defaultConfigurationPanel = getDefaultConfigurationPanel();
         _directoriesConfigurationsListPanels = new ArrayList<>(); 
        _directoriesConfigurationsPanel = getMainDirectoriesConfigurationsPanel();
        _directConnectionsListPanels = new ArrayList<>(); 
        _directConnectionsConfigurationsPanel = getDirectConnectionsConfigurationsPanel();

        
    }

    private DCPanel getDefaultConfigurationPanel() {
        
        final DCPanel defaultConfigPanel = new DCPanel().setTitledBorder(
                "Default configuration HADOOP_CONF_DIR/YARN_CONF_DIR");
        defaultConfigPanel.setLayout(new GridBagLayout());
        final DCLabel directoryConfigurationLabel1 = DCLabel.dark("Directory 1:");
        final JXTextField directoryConfigurationTextField1 =  WidgetFactory.createTextField("<none>");  
        final DCLabel directoryConfigurationLabel2 = DCLabel.dark("Directory 2:");
        final JXTextField directoryConfigurationTextField2 = WidgetFactory.createTextField("<none>");
        
        final EnvironmentBasedHadoopClusterInformation defaultServer = (EnvironmentBasedHadoopClusterInformation) _serverInformationCatalog
                .getServer(HadoopResource.DEFAULT_CLUSTERREFERENCE);
        final String[] directories = defaultServer.getDirectories();
        if (directories.length > 0) {
            directoryConfigurationTextField1.setText(directories[0]);
            if (directories.length == 2) {
                directoryConfigurationTextField2.setText(directories[1]);
            }
        }

        directoryConfigurationTextField1.setEnabled(false);
        directoryConfigurationTextField2.setEnabled(false);
        directoryConfigurationTextField1.setToolTipText(null);
        directoryConfigurationTextField2.setToolTipText(null);

        WidgetUtils.addToGridBag(directoryConfigurationLabel1, defaultConfigPanel, 0, 0, GridBagConstraints.WEST);
        WidgetUtils.addToGridBag(directoryConfigurationTextField1, defaultConfigPanel, 1, 0, 1, 1, GridBagConstraints.EAST,4, 1, 0);
        WidgetUtils.addToGridBag(directoryConfigurationLabel2, defaultConfigPanel, 0, 1,GridBagConstraints.WEST);
        WidgetUtils.addToGridBag(directoryConfigurationTextField2, defaultConfigPanel, 1, 1, 1, 1, GridBagConstraints.EAST, 4, 1, 0);

        return defaultConfigPanel;
    }

    private DCPanel getDirectConnectionsConfigurationsPanel() {
        
        final DCPanel directConnectionsPanel = new DCPanel().setTitledBorder("Direct connections to namenode");
        directConnectionsPanel.setLayout(new GridBagLayout());
        final DCLabel subTitleLabel = DCLabel.dark("Connections:");
        subTitleLabel.setFont(WidgetUtils.FONT_HEADER2);
        
        WidgetUtils.addToGridBag(subTitleLabel, directConnectionsPanel, 1, 0, 1.0, 0.0);
        
        final String[] serverNames = _serverInformationCatalog.getServerNames();
        for (int i = 0; i < serverNames.length; i++) {
            if (_serverInformationCatalog.getServer(serverNames[i]) instanceof DirectConnectionHadoopClusterInformation) {
                // create panel with this server; 
                DirectConnectionHadoopClusterInformation server = (DirectConnectionHadoopClusterInformation) _serverInformationCatalog.getServer(serverNames[i]); 
                final HadoopDirectConnectionPanel panel = new HadoopDirectConnectionPanel(server.getName(), server.getNameNodeUri(), server.getDescription(), server,_serverInformationCatalog);
                _directConnectionsListPanels.add(panel);  
            
                System.out.println("Namenode Connection : " + server.getNameNodeUri());
            }
        }
        
        int row = 0; 
        for (row = 0; row <_directConnectionsListPanels.size(); row++){
            // +1  to the grid positions because a label was added before
            WidgetUtils.addToGridBag(_directConnectionsListPanels.get(row), directConnectionsPanel, 1, row + 1, 1.0, 0.0);
        }
        
        final JButton addButton = WidgetFactory.createDefaultButton("Add"); 
        addButton.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
              
                final HadoopConnectionToNamenodeDialog directConnectionDialog = new HadoopConnectionToNamenodeDialog(_windowContext, null,  _serverInformationCatalog);
                directConnectionDialog.setVisible(true);
                if (directConnectionDialog.getSavedServer() != null){
                    System.out.println("The saved server is " + directConnectionDialog.getSavedServer().getName());
                }
                
            }
        });
        
        
        
        WidgetUtils.addToGridBag(addButton, directConnectionsPanel, 1, ++row, 1.0, 0.0); 
        return directConnectionsPanel;
    }

    private JPanel getMainDirectoriesConfigurationsPanel() {

        final DCPanel directoryConfigurationPanel = new DCPanel().setTitledBorder("Load configuration from directories:");
        final JScrollPane scrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        directoryConfigurationPanel.add(scrollPane); 
        directoryConfigurationPanel.setLayout(new GridBagLayout());
        
        final DCLabel label = DCLabel.dark("Existing Directories:");
        label.setFont(WidgetUtils.FONT_HEADER2);
       
        final JPanel centerPanel = new DCPanel();
         centerPanel.setLayout(new VerticalLayout());
        createDirectoriesConfigurationListPanel(centerPanel);
        
        for (int i=0; i<_directoriesConfigurationsListPanels.size(); i++){
            centerPanel.add(_directoriesConfigurationsListPanels.get(i).getMainPanel()); 
        }
        
        final JButton addPath = WidgetFactory.createDefaultButton("Add");
        addPath.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                final DirectoryBasedHadoopClusterInformation newDirectoryServer = new DirectoryBasedHadoopClusterInformation("New", null); 
                final DirectoryConfigurationPanel newDirConfigPanel = new DirectoryConfigurationPanel(newDirectoryServer, centerPanel);
                _directoriesConfigurationsListPanels.add(newDirConfigPanel); 
                centerPanel.add(newDirConfigPanel.getMainPanel()); 
            }
        });
        
        
        centerPanel.addContainerListener(new ContainerListener() {
            
            @Override
            public void componentRemoved(ContainerEvent e) {
                revalidate();
                repaint();
            }
            
            @Override
            public void componentAdded(ContainerEvent e) {
                revalidate();
                repaint();
            }
        });
        directoryConfigurationPanel.revalidate();
       
        WidgetUtils.addToGridBag(label, directoryConfigurationPanel, 0, 0, 1, 1, GridBagConstraints.WEST, 4, 1, 0);
        WidgetUtils.addToGridBag(centerPanel, directoryConfigurationPanel, 0, 1, 1, 1, GridBagConstraints.WEST, 4, 1, 0);
        WidgetUtils.addToGridBag(addPath, directoryConfigurationPanel, 0, 2, 0, 0, GridBagConstraints.SOUTH);
        
        return directoryConfigurationPanel;
    }

    
    /**
     * Create a list with directory configurations panels
     * @param parentPanel
     */
    private void createDirectoriesConfigurationListPanel(JPanel parentPanel) {
     
        final String[] serverNames = _serverInformationCatalog.getServerNames();
        for (int i = 0; i < serverNames.length; i++) {
            if (_serverInformationCatalog.getServer(serverNames[i]).getClass().equals(
                    DirectoryBasedHadoopClusterInformation.class)) {
                // create panel with this server;
                final DirectoryBasedHadoopClusterInformation server = (DirectoryBasedHadoopClusterInformation) _serverInformationCatalog
                        .getServer(serverNames[i]);
                _directoriesConfigurationsListPanels.add(new DirectoryConfigurationPanel(server, parentPanel)); 
            }
        }
        
    }

    @Override
    public String getWindowTitle() {
        return "Hadoop configurations";
    }

    @Override
    public Image getWindowIcon() {
        return imageManager.getImageIcon(IconUtils.FILE_HDFS).getImage();
    }

    @Override
    protected boolean isWindowResizable() {
        return true;
    }

    @Override
    protected boolean isCentered() {
        return true;
    }
    
    @Override
    public void onAdd(ServerInformation serverInformation) {
       System.out.println("The server " + serverInformation.getName() + "was added");
        
    }

    @Override
    public void onRemove(ServerInformation serverInformation) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected JComponent getWindowContent() {
        final JButton closeButton = WidgetFactory.createPrimaryButton("Close", IconUtils.ACTION_CLOSE_BRIGHT);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HadoopConfigurationsDialog.this.dispose();
                // _userPreferences.save();
            }
        });

        final DCPanel buttonPanel = DCPanel.flow(Alignment.CENTER, closeButton);

        final DCBannerPanel banner = new DCBannerPanel("Set Hadoop Configurations");

        final DCPanel panel = new DCPanel(WidgetUtils.COLOR_DEFAULT_BACKGROUND);
        panel.setLayout(new BorderLayout());
        panel.add(banner, BorderLayout.NORTH);

        final DCPanel contentPanel = new DCPanel();
        contentPanel.setLayout(new VerticalLayout());
        contentPanel.add(_defaultConfigurationPanel);
        contentPanel.add(_directoriesConfigurationsPanel);
        contentPanel.add(_directConnectionsConfigurationsPanel);
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(contentPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        panel.setPreferredSize(900, 900);
        return panel;
    }

    public static void main(String[] args) throws Exception {
        LookAndFeelManager.get().init();
        final List<ServerInformation> servers = new ArrayList<>();
        servers.add(new EnvironmentBasedHadoopClusterInformation(HadoopResource.DEFAULT_CLUSTERREFERENCE,
                "hadoop conf dir"));

        servers.add(new DirectConnectionHadoopClusterInformation("namenode", "directconnection", new URI(
                "hdfs://192.168.0.200:9000/")));
        servers.add(new DirectConnectionHadoopClusterInformation("namenode2", "directconnection", new URI(
                "hdfs://192.168.0.200:9000/")));
        final ServerInformationCatalog serverInformationCatalog = new ServerInformationCatalogImpl(servers);
        JFrame frame = new JFrame();
        frame.setVisible(false);
        frame.pack();
        frame.dispose();
        final Resource resource = new FileResource(new File("C:\\Users\\claudiap\\Desktop\\testconf.xml"));
        MutableServerInformationCatalog mutableServerInformationCatalog = new MutableServerInformationCatalog(serverInformationCatalog, new DatastoreXmlExternalizer(resource)); 

        final UserPreferencesImpl userPreferencesImpl = new UserPreferencesImpl(null);
        WindowContext windowContext = new DCWindowContext(null, null, null);

        final HadoopConfigurationsDialog hadoopConfigurationDialog = new HadoopConfigurationsDialog(windowContext, null,
                userPreferencesImpl, mutableServerInformationCatalog);
        hadoopConfigurationDialog.setVisible(true);
        hadoopConfigurationDialog.show();
        hadoopConfigurationDialog.pack();
    }

   

}
