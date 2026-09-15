package routeoptimizer;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


public class RouteOptimizerGUI extends JFrame implements ActionListener {

    static final String EXE_PATH = "C:\\Users\\Min Technology\\Desktop\\Program C\\RouteEngine\\graph_algo.exe";

    /* time complexity of each algorithm, for the comparison feature */
    static final HashMap<String, String> TIME_COMPLEXITY = new HashMap<>();
    static {
        TIME_COMPLEXITY.put("Dijkstra", "O(V^2)");
        TIME_COMPLEXITY.put("Bellman-Ford", "O(V * E)");
        TIME_COMPLEXITY.put("Floyd-Warshall", "O(V^3)");
    }

    /* ----- data kept by the GUI ----- */
    ArrayList<String> locations = new ArrayList<>();

    class Edge {
        int from, to, dist, cost, time;
        Edge(int f, int t, int d, int c, int tm) {
            from = f; to = t; dist = d; cost = c; time = tm;
        }
    }
    ArrayList<Edge> edges = new ArrayList<>();

    class AlgoResult {
        String algorithm = "";
        String path = "";
        boolean ok = false;
        int totalDistance, totalCost, totalTime;
        double executionTimeMs;
    }

    /* one alternative real-world route between source and destination,
       found independently of which algorithm was chosen               */
    class RouteOption {
        String path = "";
        int distance, cost, time;
        boolean isBest;
    }

    /* ----- Locations tab ----- */
    JTextField locationField;
    DefaultListModel<String> locationListModel;
    JList<String> locationList;
    JButton addLocationButton;
    JButton updateLocationButton;
    JButton removeLocationButton;

    /* ----- Routes tab ----- */
    JComboBox<String> fromCombo, toCombo;
    JTextField distField, costField, timeField;
    DefaultListModel<String> edgeListModel;
    JList<String> edgeList;
    JButton addEdgeButton;
    JButton removeEdgeButton;
    JButton updateEdgeButton;

    /* ----- Find Route tab ----- */
    JComboBox<String> sourceCombo, destCombo, criteriaCombo, algoCombo;
    JButton findRouteButton;

    /* ----- Result area ----- */
    JTextArea outputArea;

    public RouteOptimizerGUI() {
        setTitle("Intelligent Route Optimization System Using Graph Algorithms");
        setSize(780, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JLabel titleLabel = new JLabel(
                "Intelligent Route Optimization System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setBorder(new EmptyBorder(12, 10, 12, 10));
        add(titleLabel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1. Locations", buildLocationPanel());
        tabs.addTab("2. Routes", buildEdgePanel());
        tabs.addTab("3. Find Route", buildFindRoutePanel());
        add(tabs, BorderLayout.CENTER);

        add(buildOutputPanel(), BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    /* small helper so every tab has the same clean spacing */
    private JPanel row(Component... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        for (Component c : parts) p.add(c);
        return p;
    }

    /* ---------------- tab 1: locations ---------------- */
    private JPanel buildLocationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        locationField = new JTextField(20);
        addLocationButton = new JButton("Add Location");
        addLocationButton.addActionListener(this);
        updateLocationButton = new JButton("Update Selected Location");
        updateLocationButton.addActionListener(this);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(row(new JLabel("Location name:"), locationField));
        form.add(row(addLocationButton, updateLocationButton));
        panel.add(form, BorderLayout.NORTH);

        locationListModel = new DefaultListModel<>();
        locationList = new JList<>(locationListModel);
        locationList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        /* clicking a location loads its name into the field above, so you
           can change it and click Update Selected Location to save        */
        locationList.addListSelectionListener(ev -> {
            if (!ev.getValueIsAdjusting()) loadLocationIntoForm();
        });
        JScrollPane scroll = new JScrollPane(locationList);
        scroll.setBorder(new TitledBorder("Locations added so far  (click one to edit or delete it)"));
        panel.add(scroll, BorderLayout.CENTER);

        removeLocationButton = new JButton("Remove Selected Location");
        removeLocationButton.addActionListener(this);
        panel.add(row(removeLocationButton), BorderLayout.SOUTH);

        return panel;
    }

    /* ---------------- tab 2: routes(edges) ---------------- */
    private JPanel buildEdgePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        fromCombo = new JComboBox<>();
        toCombo = new JComboBox<>();
        form.add(row(new JLabel("Source:"), fromCombo,
                new JLabel("      Destination:"), toCombo));

        distField = new JTextField(6);
        costField = new JTextField(6);
        timeField = new JTextField(6);
        form.add(row(
                new JLabel("Distance (km):"), distField,
                new JLabel("Cost (Taka):"), costField,
                new JLabel("Time (min):"), timeField));

        addEdgeButton = new JButton("Add Route");
        addEdgeButton.addActionListener(this);
        updateEdgeButton = new JButton("Update Selected Route");
        updateEdgeButton.addActionListener(this);
        form.add(row(addEdgeButton, updateEdgeButton));

        panel.add(form, BorderLayout.NORTH);

        edgeListModel = new DefaultListModel<>();
        edgeList = new JList<>(edgeListModel);
        edgeList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        /* clicking a route loads its values into the form above, so you
           can change them and click Update Selected Route to save        */
        edgeList.addListSelectionListener(ev -> {
            if (!ev.getValueIsAdjusting()) loadEdgeIntoForm();
        });
        JScrollPane scroll = new JScrollPane(edgeList);
        scroll.setBorder(new TitledBorder(
                "Routes added so far  (click one to edit or delete it)"));
        panel.add(scroll, BorderLayout.CENTER);

        removeEdgeButton = new JButton("Remove Selected Route");
        removeEdgeButton.addActionListener(this);
        panel.add(row(removeEdgeButton), BorderLayout.SOUTH);

        return panel;
    }

    /* ---------------- tab 3: find the optimal route ---------------- */
    private JPanel buildFindRoutePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 15, 15, 15));

        sourceCombo = new JComboBox<>();
        destCombo = new JComboBox<>();
        panel.add(row(new JLabel("Source Location:"), sourceCombo,
                new JLabel("      Destination Location:"), destCombo));

        criteriaCombo = new JComboBox<>(
                new String[]{"Minimum Distance", "Minimum Cost", "Minimum Travel Time"});
        panel.add(row(new JLabel("Optimization Criterion:"), criteriaCombo));

        algoCombo = new JComboBox<>(new String[]{
                "Dijkstra", "Bellman-Ford", "Floyd-Warshall", "Compare All Algorithms"});
        panel.add(row(new JLabel("Algorithm:"), algoCombo));

        findRouteButton = new JButton("Find Optimal Route");
        findRouteButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        findRouteButton.addActionListener(this);
        panel.add(row(findRouteButton));

        return panel;
    }

    /* ---------------- result area (always visible) ---------------- */
    private JPanel buildOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Result"));

        outputArea = new JTextArea(14, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        panel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        return panel;
    }

    private void refreshLocationCombos() {
        fromCombo.removeAllItems();
        toCombo.removeAllItems();
        sourceCombo.removeAllItems();
        destCombo.removeAllItems();
        for (String loc : locations) {
            fromCombo.addItem(loc);
            toCombo.addItem(loc);
            sourceCombo.addItem(loc);
            destCombo.addItem(loc);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addLocationButton) {
            addLocation();
        } else if (e.getSource() == updateLocationButton) {
            updateLocation();
        } else if (e.getSource() == removeLocationButton) {
            removeLocation();
        } else if (e.getSource() == addEdgeButton) {
            addEdge();
        } else if (e.getSource() == removeEdgeButton) {
            removeEdge();
        } else if (e.getSource() == updateEdgeButton) {
            updateEdge();
        } else if (e.getSource() == findRouteButton) {
            findRoute();
        }
    }

    private void addLocation() {
        String name = locationField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type a location name.");
            return;
        }
        if (name.contains(" ")) {
            JOptionPane.showMessageDialog(this,
                    "Location names cannot contain spaces. Use underscores instead, e.g. Bus_Station");
            return;
        }
        if (locations.contains(name)) {
            JOptionPane.showMessageDialog(this, "This location already exists.");
            return;
        }

        locations.add(name);
        locationListModel.addElement(name);
        refreshLocationCombos();
        locationField.setText("");
    }

    /* called automatically when the user clicks a location in the list */
    private void loadLocationIntoForm() {
        int selected = locationList.getSelectedIndex();
        if (selected == -1) return;
        locationField.setText(locations.get(selected));
    }

    /* renames whichever location is currently selected */
    private void updateLocation() {
        int selected = locationList.getSelectedIndex();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this,
                    "Click a location in the list first to select which one to rename.");
            return;
        }

        String newName = locationField.getText().trim();
        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type a location name.");
            return;
        }
        if (newName.contains(" ")) {
            JOptionPane.showMessageDialog(this,
                    "Location names cannot contain spaces. Use underscores instead, e.g. Bus_Station");
            return;
        }
        /* allow keeping the same name, but block renaming to a DIFFERENT
           location's existing name */
        for (int i = 0; i < locations.size(); i++) {
            if (i != selected && locations.get(i).equals(newName)) {
                JOptionPane.showMessageDialog(this, "Another location already has this name.");
                return;
            }
        }

        locations.set(selected, newName);
        locationListModel.set(selected, newName);

        /* the routes list and the dropdowns show location names as plain
           text, so they need to be rebuilt to show the new name too      */
        refreshLocationCombos();
        refreshEdgeListDisplay();

        JOptionPane.showMessageDialog(this, "Location updated.");
    }

    /* deletes whichever location is currently selected - but only if no
       route is still using it, so we can never leave a broken route behind */
    private void removeLocation() {
        int selected = locationList.getSelectedIndex();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this,
                    "Click a location in the list first, then click Remove Selected Location.");
            return;
        }

        for (Edge ed : edges) {
            if (ed.from == selected || ed.to == selected) {
                JOptionPane.showMessageDialog(this,
                        "This location is used in one or more routes. Remove those routes first "
                                + "(in the Routes tab), then delete this location.");
                return;
            }
        }

        locations.remove(selected);
        locationListModel.remove(selected);

        /* every route pointing at a location AFTER the one we removed
           needs its index shifted down by one, since the list moved     */
        for (Edge ed : edges) {
            if (ed.from > selected) ed.from--;
            if (ed.to > selected) ed.to--;
        }

        refreshLocationCombos();
        refreshEdgeListDisplay();
        locationField.setText("");
    }

    /* rebuilds the routes list text from scratch using the current
       location names and edge data - used after a rename or a delete   */
    private void refreshEdgeListDisplay() {
        edgeListModel.clear();
        for (Edge ed : edges) {
            edgeListModel.addElement(locations.get(ed.from) + "  ->  " + locations.get(ed.to)
                    + "     (Distance=" + ed.dist + " km,  Cost=" + ed.cost
                    + " Taka,  Time=" + ed.time + " min)");
        }
    }

    private void addEdge() {
        if (locations.size() < 2) {
            JOptionPane.showMessageDialog(this, "Add at least two locations first.");
            return;
        }

        int from = fromCombo.getSelectedIndex();
        int to = toCombo.getSelectedIndex();

        if (from == to) {
            JOptionPane.showMessageDialog(this, "Source and destination cannot be the same location.");
            return;
        }

        int dist, cost, time;
        try {
            dist = Integer.parseInt(distField.getText().trim());
            cost = Integer.parseInt(costField.getText().trim());
            time = Integer.parseInt(timeField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Distance, cost and time must be whole numbers.");
            return;
        }

        if (dist < 0 || cost < 0 || time < 0) {
            JOptionPane.showMessageDialog(this, "Distance, cost and time cannot be negative.");
            return;
        }

        edges.add(new Edge(from, to, dist, cost, time));
        edgeListModel.addElement(locations.get(from) + "  ->  " + locations.get(to)
                + "     (Distance=" + dist + " km,  Cost=" + cost + " Taka,  Time=" + time + " min)");

        distField.setText("");
        costField.setText("");
        timeField.setText("");
    }

    /* deletes whichever route is currently selected in the routes list */
    private void removeEdge() {
        int selected = edgeList.getSelectedIndex();

        if (selected == -1) {
            JOptionPane.showMessageDialog(this,
                    "Click a route in the list first, then click Remove Selected Route.");
            return;
        }

        edges.remove(selected);
        edgeListModel.remove(selected);
    }

    /* called automatically when the user clicks a route in the list -
       fills the form fields above with that route's current values so
       they can be edited                                              */
    private void loadEdgeIntoForm() {
        int selected = edgeList.getSelectedIndex();
        if (selected == -1) return;

        Edge ed = edges.get(selected);
        fromCombo.setSelectedIndex(ed.from);
        toCombo.setSelectedIndex(ed.to);
        distField.setText(String.valueOf(ed.dist));
        costField.setText(String.valueOf(ed.cost));
        timeField.setText(String.valueOf(ed.time));
    }

    /* saves the form's current values into whichever route is selected */
    private void updateEdge() {
        int selected = edgeList.getSelectedIndex();

        if (selected == -1) {
            JOptionPane.showMessageDialog(this,
                    "Click a route in the list first to select which one to update.");
            return;
        }

        int from = fromCombo.getSelectedIndex();
        int to = toCombo.getSelectedIndex();

        if (from == to) {
            JOptionPane.showMessageDialog(this, "Source and destination cannot be the same location.");
            return;
        }

        int dist, cost, time;
        try {
            dist = Integer.parseInt(distField.getText().trim());
            cost = Integer.parseInt(costField.getText().trim());
            time = Integer.parseInt(timeField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Distance, cost and time must be whole numbers.");
            return;
        }

        if (dist < 0 || cost < 0 || time < 0) {
            JOptionPane.showMessageDialog(this, "Distance, cost and time cannot be negative.");
            return;
        }

        edges.set(selected, new Edge(from, to, dist, cost, time));
        edgeListModel.set(selected, locations.get(from) + "  ->  " + locations.get(to)
                + "     (Distance=" + dist + " km,  Cost=" + cost + " Taka,  Time=" + time + " min)");

        JOptionPane.showMessageDialog(this, "Route updated.");
    }

    
    
    private void findRoute() {
        if (locations.size() < 2) {
            JOptionPane.showMessageDialog(this, "Add at least two locations first.");
            return;
        }
        if (edges.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one route first.");
            return;
        }

        int source = sourceCombo.getSelectedIndex();
        int dest = destCombo.getSelectedIndex();

        if (source == dest) {
            JOptionPane.showMessageDialog(this, "Source and destination cannot be the same.");
            return;
        }

        int criteria = criteriaCombo.getSelectedIndex() + 1;   // 1=Distance,2=Cost,3=Time
        int algo = algoCombo.getSelectedIndex() + 1;           

        try {
            writeInputFile("graph.txt", source, dest, criteria, algo);
            runCProgram("graph.txt", "result.txt");
            ArrayList<RouteOption> routeOptions = parseRouteOptions("result.txt");
            ArrayList<AlgoResult> results = parseResultFile("result.txt");
            displayResults(results, routeOptions, criteria, algo);
        } catch (IOException | InterruptedException ex) {
            outputArea.setText("Error while running the algorithm engine:\n" + ex.getMessage());
        }
    }


    private void writeInputFile(String filename, int source, int dest, int criteria, int algo)
            throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(filename));

        pw.println(locations.size() + " " + edges.size());
        for (String loc : locations) {
            pw.println(loc);
        }
        for (Edge ed : edges) {
            pw.println(ed.from + " " + ed.to + " " + ed.dist + " " + ed.cost + " " + ed.time);
        }
        pw.println(source + " " + dest + " " + criteria + " " + algo);

        pw.close();
    }

    /* runs the compiled C program and waits for it to finish */
    private void runCProgram(String inputFile, String outputFile)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(EXE_PATH, inputFile, outputFile);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while (reader.readLine() != null) {
            // just draining the stream; graph_algo.exe does not normally print anything
        }
        reader.close();

        process.waitFor();
    }

    
    
    /* reads result.txt and turns it into a list of AlgoResult objects */
    
    private ArrayList<RouteOption> parseRouteOptions(String outputFile) throws IOException {
        ArrayList<RouteOption> options = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(outputFile));
        String line;
        boolean insideBlock = false;
        RouteOption current = null;

        while ((line = reader.readLine()) != null) {
            if (line.equals("ALL_ROUTES_START")) {
                insideBlock = true;
            } else if (line.equals("ALL_ROUTES_END")) {
                insideBlock = false;
            } else if (insideBlock) {
                if (line.startsWith("ROUTE_PATH=")) {
                    current = new RouteOption();
                    current.path = line.substring(11);
                } else if (line.startsWith("ROUTE_DISTANCE=") && current != null) {
                    current.distance = Integer.parseInt(line.substring(15));
                } else if (line.startsWith("ROUTE_COST=") && current != null) {
                    current.cost = Integer.parseInt(line.substring(11));
                } else if (line.startsWith("ROUTE_TIME=") && current != null) {
                    current.time = Integer.parseInt(line.substring(11));
                } else if (line.startsWith("ROUTE_BEST=") && current != null) {
                    current.isBest = line.substring(11).equals("YES");
                    options.add(current);
                }
            }
        }
        reader.close();
        return options;
    }

    private ArrayList<AlgoResult> parseResultFile(String outputFile) throws IOException {
        ArrayList<AlgoResult> results = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(outputFile));
        String line;
        AlgoResult current = new AlgoResult();

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("ALGORITHM=")) {
                current.algorithm = line.substring(10);
            } else if (line.startsWith("PATH=")) {
                current.path = line.substring(5);
            } else if (line.startsWith("STATUS=")) {
                current.ok = line.substring(7).equals("OK");
            } else if (line.startsWith("TOTAL_DISTANCE=")) {
                current.totalDistance = Integer.parseInt(line.substring(15));
            } else if (line.startsWith("TOTAL_COST=")) {
                current.totalCost = Integer.parseInt(line.substring(11));
            } else if (line.startsWith("TOTAL_TIME=")) {
                current.totalTime = Integer.parseInt(line.substring(11));
            } else if (line.startsWith("EXECUTION_TIME_MS=")) {
                current.executionTimeMs = Double.parseDouble(line.substring(19));
            } else if (line.equals("----")) {
                results.add(current);
                current = new AlgoResult();
            }
        }
        reader.close();
        return results;
    }


    private void displayResults(ArrayList<AlgoResult> results, ArrayList<RouteOption> routeOptions,
                                 int criteria, int algoChoice) {
        StringBuilder sb = new StringBuilder();

        if (algoChoice != 4) {
            AlgoResult r = results.get(0);

            if (!r.ok) {
                sb.append("No possible route exists between these two locations.\n");
            } else {
                sb.append("Optimal Route  : ").append(r.path).append("\n");
                sb.append("Total Distance : ").append(r.totalDistance).append(" km\n");
                sb.append("Total Cost     : ").append(r.totalCost).append(" Taka\n");
                sb.append("Total Time     : ").append(r.totalTime).append(" min\n");
                sb.append("Algorithm      : ").append(r.algorithm).append("\n");
                sb.append("Execution Time : ").append(String.format("%.5f", r.executionTimeMs)).append(" ms\n");
            }
        } else {
            /* ---------- algorithm comparison ---------- */
            sb.append("===== Algorithm Comparison =====\n");
            sb.append("Graph size: ").append(locations.size()).append(" locations, ")
                    .append(edges.size()).append(" routes\n");
            sb.append("Optimization criterion: ").append(criteriaCombo.getSelectedItem()).append("\n\n");

            for (AlgoResult r : results) {
                sb.append("Algorithm       : ").append(r.algorithm).append("\n");

                if (!r.ok) {
                    sb.append("Route Found     : No possible route\n");
                } else {
                    sb.append("Route Found     : ").append(r.path).append("\n");

                    /* "Total Weight" = whichever value the chosen criterion optimized for */
                    String weightLine;
                    if (criteria == 1) weightLine = r.totalDistance + " km";
                    else if (criteria == 2) weightLine = r.totalCost + " Taka";
                    else weightLine = r.totalTime + " min";
                    sb.append("Total Weight    : ").append(weightLine).append("\n");

                    sb.append("(full breakdown  : distance=").append(r.totalDistance)
                            .append(" km, cost=").append(r.totalCost).append(" Taka")
                            .append(", time=").append(r.totalTime).append(" min)\n");
                }

                sb.append("Execution Time  : ").append(String.format("%.5f", r.executionTimeMs)).append(" ms\n");
                sb.append("Time Complexity : ").append(TIME_COMPLEXITY.get(r.algorithm)).append("\n");
                sb.append("--------------------------------------------------\n");
            }
        }

        /* ---------- alternative routes: every real path the program found,
           not just the one the algorithm picked, so you can see WHY it won ---------- */
        sb.append("\n===== Alternative Routes Considered =====\n");

        if (routeOptions.isEmpty()) {
            sb.append("No routes at all exist between these two locations.\n");
        } else if (routeOptions.size() == 1) {
            sb.append("Only one possible route exists between these locations:\n");
            sb.append("  ").append(routeOptions.get(0).path).append("\n");
        } else {
            String criterionName = (criteria == 1) ? "Distance" : (criteria == 2) ? "Cost" : "Time";
            sb.append("Found ").append(routeOptions.size())
                    .append(" different possible routes. Ranked by ").append(criterionName)
                    .append(" (best first):\n\n");

            int rank = 1;
            for (RouteOption r : routeOptions) {
                sb.append(rank).append(". ").append(r.isBest ? "[BEST] " : "        ")
                        .append(r.path).append("\n");
                sb.append("     distance=").append(r.distance).append(" km, cost=")
                        .append(r.cost).append(" Taka, time=").append(r.time).append(" min\n");
                rank++;
            }
        }

        outputArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RouteOptimizerGUI gui = new RouteOptimizerGUI();
            gui.setVisible(true);
        });
    }
}

