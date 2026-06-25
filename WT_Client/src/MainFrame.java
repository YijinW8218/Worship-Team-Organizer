import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainFrame extends JFrame {
    private static final Color BACKGROUND = new Color(245, 247, 250);
    private static final Color PANEL_BACKGROUND = Color.WHITE;
    private static final Color BORDER = new Color(206, 212, 218);
    private static final Color TEXT = new Color(33, 37, 41);
    private static final Color MUTED_TEXT = new Color(108, 117, 125);
    private static final Color ACCENT = new Color(36, 99, 235);
    private static final Color SELECTED_DAY_BACKGROUND = new Color(229, 231, 235);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font SECTION_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter ACTIVITY_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm");

    private final ClientConnection connection;

    public MainFrame(ClientConnection connection, String username) {
        this.connection = connection;

        setTitle("Worship Team Organizer");
        setMinimumSize(new Dimension(900, 600));
        setSize(980, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeConnection();
            }
        });

        setContentPane(createContentPanel(username, connection));
    }

    public static JPanel createContentPanel(String username) {
        return createContentPanel(username, null);
    }

    private static JPanel createContentPanel(String username, ClientConnection connection) {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 22, 22));

        root.add(createHeader(username), BorderLayout.NORTH);
        root.add(createDashboard(connection), BorderLayout.CENTER);

        return root;
    }

    private static JPanel createHeader(String username) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Worship Team Organizer");
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT);

        JLabel user = new JLabel("Signed in as " + username);
        user.setFont(BODY_FONT);
        user.setForeground(MUTED_TEXT);

        header.add(title, BorderLayout.WEST);
        header.add(user, BorderLayout.EAST);
        return header;
    }

    private static JPanel createDashboard(ClientConnection connection) {
        EventInfoPanel eventInfoPanel = new EventInfoPanel();
        TeamInfoPanel teamInfoPanel = new TeamInfoPanel();
        ActivityInfoPanel activityInfoPanel = new ActivityInfoPanel();
        final LocalDate[] selectedDate = new LocalDate[1];

        DateSelectionHandler handler = date -> {
            selectedDate[0] = date;
            eventInfoPanel.setSelectedDate(date);
            if (connection == null) {
                eventInfoPanel.showEmpty();
                teamInfoPanel.showEmpty();
                return;
            }

            eventInfoPanel.showLoading(date);
            teamInfoPanel.showLoading();

            new SwingWorker<DashboardData, Void>() {
                @Override
                protected DashboardData doInBackground() throws IOException {
                    String response = connection.sendCommandForResponse("LIST_DAY_DETAIL|" + date);
                    List<DayEvent> events = parseDayDetailResponse(response);
                    List<String> activities = fetchUserActivities(connection);
                    return new DashboardData(events, activities);
                }

                @Override
                protected void done() {
                    try {
                        DashboardData data = get();
                        eventInfoPanel.showEvents(date, data.events);
                        teamInfoPanel.showEmpty();
                        activityInfoPanel.showActivities(data.activities);
                    } catch (Exception ex) {
                        eventInfoPanel.showError(ex.getMessage());
                        teamInfoPanel.showEmpty();
                    }
                }
            }.execute();
        };
        CalendarPanel calendarPanel = new CalendarPanel(handler);
        calendarPanel.setMonthChangeHandler(month -> {
            if (connection != null) {
                refreshCalendarCounts(calendarPanel, connection);
            }
        });

        AddEventHandler addEventHandler = () -> {
            if (connection == null || selectedDate[0] == null) {
                return;
            }
            showAddEventDialog(eventInfoPanel, teamInfoPanel, activityInfoPanel, calendarPanel, connection, selectedDate[0]);
        };
        eventInfoPanel.setAddEventHandler(addEventHandler);
        eventInfoPanel.setDeleteEventHandler(event -> {
            if (connection == null || selectedDate[0] == null) {
                return;
            }
            deleteSelectedEvent(eventInfoPanel, teamInfoPanel, activityInfoPanel, calendarPanel,
                    connection, selectedDate[0], event);
        });
        eventInfoPanel.setEditTopicHandler(event -> {
            if (connection == null || selectedDate[0] == null) {
                return;
            }
            showEditTopicDialog(eventInfoPanel, teamInfoPanel, activityInfoPanel, connection, selectedDate[0], event);
        });
        eventInfoPanel.setEditSongsHandler(event -> {
            if (connection == null || selectedDate[0] == null) {
                return;
            }
            showEditSongsDialog(eventInfoPanel, teamInfoPanel, activityInfoPanel, connection, selectedDate[0], event);
        });
        eventInfoPanel.setSelectedEventHandler(teamInfoPanel::showEvent);
        teamInfoPanel.setAddMemberHandler(event -> {
            if (connection == null || selectedDate[0] == null) {
                return;
            }
            showAddMemberDialog(eventInfoPanel, teamInfoPanel, activityInfoPanel,
                    connection, selectedDate[0], event);
        });
        teamInfoPanel.setDeleteMemberHandler((event, memberName) -> {
            if (connection == null || selectedDate[0] == null) {
                return;
            }
            deleteSelectedMember(eventInfoPanel, teamInfoPanel, activityInfoPanel,
                    connection, selectedDate[0], event, memberName);
        });

        if (connection != null) {
            subscribeToServerChanges(eventInfoPanel, teamInfoPanel, activityInfoPanel, calendarPanel,
                    connection, selectedDate);
            refreshUserActivity(activityInfoPanel, connection);
            refreshCalendarCounts(calendarPanel, connection);
        }

        JPanel dashboard = new JPanel(new GridBagLayout());
        dashboard.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 16, 16);
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.95;
        gbc.weighty = 1.0;
        JPanel calendarCard = createCalendarCard(calendarPanel);
        calendarCard.setMinimumSize(new Dimension(410, 360));
        calendarCard.setPreferredSize(new Dimension(420, 420));
        dashboard.add(calendarCard, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 1.15;
        gbc.weighty = 0.66;
        dashboard.add(eventInfoPanel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weighty = 0.34;
        dashboard.add(teamInfoPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weightx = 1.0;
        gbc.weighty = 0.28;
        dashboard.add(activityInfoPanel, gbc);

        return dashboard;
    }

    private static void showAddEventDialog(
            Component parent,
            TeamInfoPanel teamInfoPanel,
            ActivityInfoPanel activityInfoPanel,
            CalendarPanel calendarPanel,
            ClientConnection connection,
            LocalDate selectedDate
    ) {
        javax.swing.JTextField timeField = new javax.swing.JTextField(8);
        javax.swing.JTextField titleField = new javax.swing.JTextField(18);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Date:"), gbc);
        gbc.gridx = 1;
        panel.add(createBodyLabel(selectedDate.toString()), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Time:"), gbc);
        gbc.gridx = 1;
        panel.add(timeField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        panel.add(titleField, gbc);

        int result = JOptionPane.showConfirmDialog(
                parent,
                panel,
                "Add Event",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String time = timeField.getText().trim();
        String title = titleField.getText().trim();
        if (!isValidEventInput(parent, time, title)) {
            return;
        }

        ((EventInfoPanel) parent).showSaving(selectedDate);
        teamInfoPanel.showLoading();

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws IOException {
                String addResponse = connection.sendCommandForResponse(
                        "ADD_EVENT|" + selectedDate + "|" + time + "|" + title
                );
                if (!"ADD_EVENT_SUCCESS".equals(addResponse)) {
                    throw new IOException("Repeated event title.");
                }

                String detailResponse = connection.sendCommandForResponse("LIST_DAY_DETAIL|" + selectedDate);
                List<DayEvent> events = parseDayDetailResponse(detailResponse);
                List<String> activities = fetchUserActivities(connection);
                return new DashboardData(events, activities);
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    ((EventInfoPanel) parent).showEvents(selectedDate, data.events);
                    teamInfoPanel.showEmpty();
                    activityInfoPanel.showActivities(data.activities);
                    refreshCalendarCounts(calendarPanel, connection);
                } catch (Exception ex) {
                    ((EventInfoPanel) parent).showError(ex.getMessage());
                    teamInfoPanel.showEmpty();
                }
            }
        }.execute();
    }

    private static void deleteSelectedEvent(
            EventInfoPanel eventInfoPanel,
            TeamInfoPanel teamInfoPanel,
            ActivityInfoPanel activityInfoPanel,
            CalendarPanel calendarPanel,
            ClientConnection connection,
            LocalDate selectedDate,
            DayEvent event
    ) {
        eventInfoPanel.showDeleting(selectedDate);
        teamInfoPanel.showLoading();

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws IOException {
                String removeResponse = connection.sendCommandForResponse("REMOVE_EVENT|" + event.title);
                if (!"REMOVE_EVENT_SUCCESS".equals(removeResponse)) {
                    throw new IOException("Could not remove event.");
                }

                String detailResponse = connection.sendCommandForResponse("LIST_DAY_DETAIL|" + selectedDate);
                List<DayEvent> events = parseDayDetailResponse(detailResponse);
                List<String> activities = fetchUserActivities(connection);
                return new DashboardData(events, activities);
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    eventInfoPanel.showEvents(selectedDate, data.events);
                    teamInfoPanel.showEmpty();
                    activityInfoPanel.showActivities(data.activities);
                    refreshCalendarCounts(calendarPanel, connection);
                    refreshUserActivity(activityInfoPanel, connection);
                } catch (Exception ex) {
                    eventInfoPanel.showError(ex.getMessage());
                    teamInfoPanel.showEmpty();
                }
            }
        }.execute();
    }

    private static void showEditTopicDialog(
            EventInfoPanel eventInfoPanel,
            TeamInfoPanel teamInfoPanel,
            ActivityInfoPanel activityInfoPanel,
            ClientConnection connection,
            LocalDate selectedDate,
            DayEvent event
    ) {
        javax.swing.JTextField topicField = new javax.swing.JTextField(event.topic, 24);
        int result = JOptionPane.showConfirmDialog(
                eventInfoPanel,
                topicField,
                "Edit Topic",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        eventInfoPanel.showSavingTopic(selectedDate);
        teamInfoPanel.showLoading();

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws IOException {
                String topic = Base64.getEncoder().encodeToString(
                        topicField.getText().getBytes(StandardCharsets.UTF_8)
                );
                String response = connection.sendCommandForResponse("EDIT_TOPIC|" + event.title + "|" + topic);
                if (!"EDIT_TOPIC_SUCCESS".equals(response)) {
                    throw new IOException("Could not update topic.");
                }

                String detailResponse = connection.sendCommandForResponse("LIST_DAY_DETAIL|" + selectedDate);
                List<DayEvent> events = parseDayDetailResponse(detailResponse);
                List<String> activities = fetchUserActivities(connection);
                return new DashboardData(events, activities);
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    eventInfoPanel.showEvents(selectedDate, data.events);
                    teamInfoPanel.showEmpty();
                    activityInfoPanel.showActivities(data.activities);
                } catch (Exception ex) {
                    eventInfoPanel.showError(ex.getMessage());
                    teamInfoPanel.showEmpty();
                }
            }
        }.execute();
    }

    private static void showEditSongsDialog(
            EventInfoPanel eventInfoPanel,
            TeamInfoPanel teamInfoPanel,
            ActivityInfoPanel activityInfoPanel,
            ClientConnection connection,
            LocalDate selectedDate,
            DayEvent event
    ) {
        SongsEditorPanel editorPanel = new SongsEditorPanel(event.songs);
        int result = JOptionPane.showConfirmDialog(
                eventInfoPanel,
                editorPanel,
                "Edit Songs",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        List<SongDraft> editedSongs;
        try {
            editedSongs = editorPanel.getSongs();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(eventInfoPanel, ex.getMessage(), "Invalid Song", JOptionPane.ERROR_MESSAGE);
            return;
        }

        eventInfoPanel.showSavingSongs(selectedDate);
        teamInfoPanel.showLoading();

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws IOException {
                syncSongs(connection, event, editedSongs);

                String detailResponse = connection.sendCommandForResponse("LIST_DAY_DETAIL|" + selectedDate);
                List<DayEvent> events = parseDayDetailResponse(detailResponse);
                List<String> activities = fetchUserActivities(connection);
                return new DashboardData(events, activities);
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    eventInfoPanel.showEvents(selectedDate, data.events);
                    teamInfoPanel.showEmpty();
                    activityInfoPanel.showActivities(data.activities);
                } catch (Exception ex) {
                    eventInfoPanel.showError(ex.getMessage());
                    teamInfoPanel.showEmpty();
                }
            }
        }.execute();
    }

    private static void showAddMemberDialog(
            EventInfoPanel eventInfoPanel,
            TeamInfoPanel teamInfoPanel,
            ActivityInfoPanel activityInfoPanel,
            ClientConnection connection,
            LocalDate selectedDate,
            DayEvent event
    ) {
        javax.swing.JTextField nameField = new javax.swing.JTextField(18);
        javax.swing.JTextField roleField = new javax.swing.JTextField(18);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Position:"), gbc);
        gbc.gridx = 1;
        panel.add(roleField, gbc);

        int result = JOptionPane.showConfirmDialog(
                teamInfoPanel,
                panel,
                "Add Member",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String memberName = nameField.getText().trim();
        String role = roleField.getText().trim();
        if (!isValidMemberInput(teamInfoPanel, memberName, role)) {
            return;
        }

        teamInfoPanel.showLoading();

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws IOException {
                String response = connection.sendCommandForResponse(
                        "ADD_MEMBER|" + event.title + "|" + memberName + "|" + role
                );
                if (!"ADD_MEMBER_SUCCESS".equals(response)) {
                    throw new IOException("Could not add member.");
                }

                String detailResponse = connection.sendCommandForResponse("LIST_DAY_DETAIL|" + selectedDate);
                List<DayEvent> events = parseDayDetailResponse(detailResponse);
                List<String> activities = fetchUserActivities(connection);
                return new DashboardData(events, activities);
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    eventInfoPanel.showEvents(selectedDate, data.events);
                    DayEvent updatedEvent = findEventByTitle(data.events, event.title);
                    eventInfoPanel.selectEventByTitle(event.title);
                    teamInfoPanel.showEvent(updatedEvent);
                    activityInfoPanel.showActivities(data.activities);
                } catch (Exception ex) {
                    teamInfoPanel.showError(ex.getMessage());
                }
            }
        }.execute();
    }

    private static void deleteSelectedMember(
            EventInfoPanel eventInfoPanel,
            TeamInfoPanel teamInfoPanel,
            ActivityInfoPanel activityInfoPanel,
            ClientConnection connection,
            LocalDate selectedDate,
            DayEvent event,
            String memberName
    ) {
        teamInfoPanel.showLoading();

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws IOException {
                String response = connection.sendCommandForResponse(
                        "REMOVE_MEMBER|" + event.title + "|" + memberName
                );
                if (!"REMOVE_MEMBER_SUCCESS".equals(response)) {
                    throw new IOException("Could not remove member.");
                }

                String detailResponse = connection.sendCommandForResponse("LIST_DAY_DETAIL|" + selectedDate);
                List<DayEvent> events = parseDayDetailResponse(detailResponse);
                List<String> activities = fetchUserActivities(connection);
                return new DashboardData(events, activities);
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    eventInfoPanel.showEvents(selectedDate, data.events);
                    DayEvent updatedEvent = findEventByTitle(data.events, event.title);
                    eventInfoPanel.selectEventByTitle(event.title);
                    teamInfoPanel.showEvent(updatedEvent);
                    activityInfoPanel.showActivities(data.activities);
                } catch (Exception ex) {
                    teamInfoPanel.showError(ex.getMessage());
                }
            }
        }.execute();
    }

    private static void syncSongs(ClientConnection connection, DayEvent event, List<SongDraft> editedSongs)
            throws IOException {
        Map<String, SongDraft> originalByName = songsByName(parseSongs(event.songs));
        Map<String, SongDraft> editedByName = songsByName(editedSongs);

        for (SongDraft original : originalByName.values()) {
            SongDraft edited = editedByName.get(original.name.toLowerCase(Locale.ENGLISH));
            if (edited == null || !original.author.equals(edited.author)) {
                String response = connection.sendCommandForResponse("REMOVE_SONG|" + event.title + "|" + original.name);
                if (!"REMOVE_SONG_SUCCESS".equals(response)) {
                    throw new IOException("Could not remove song: " + original.name);
                }
            }
        }

        for (SongDraft edited : editedByName.values()) {
            SongDraft original = originalByName.get(edited.name.toLowerCase(Locale.ENGLISH));
            if (original == null || !original.author.equals(edited.author)) {
                String response = connection.sendCommandForResponse(
                        "ADD_SONG|" + event.title + "|" + edited.name + "|" + edited.author
                );
                if (!"ADD_SONG_SUCCESS".equals(response)) {
                    throw new IOException("Could not add song: " + edited.name);
                }
            }
        }
    }

    private static Map<String, SongDraft> songsByName(List<SongDraft> songs) {
        Map<String, SongDraft> byName = new LinkedHashMap<>();
        for (SongDraft song : songs) {
            byName.put(song.name.toLowerCase(Locale.ENGLISH), song);
        }
        return byName;
    }

    private static boolean isValidEventInput(Component parent, String time, String title) {
        if (time.isEmpty() || title.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Please enter time and title.");
            return false;
        }

        try {
            LocalTime.parse(time);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(parent, "Time format should be HH:mm, for example 16:08.");
            return false;
        }

        if (title.contains("|") || title.contains(",")) {
            JOptionPane.showMessageDialog(parent, "Title cannot contain | or comma.");
            return false;
        }

        return true;
    }

    private static boolean isValidMemberInput(Component parent, String memberName, String role) {
        if (memberName.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Please enter member name.");
            return false;
        }

        if (memberName.contains("|") || memberName.contains(",") || role.contains("|")) {
            JOptionPane.showMessageDialog(parent, "Member name cannot contain | or comma. Position cannot contain |.");
            return false;
        }

        return true;
    }

    private static JPanel createCalendarCard(CalendarPanel calendarPanel) {
        JPanel card = createCard("Calendar");
        card.add(calendarPanel, BorderLayout.CENTER);
        return card;
    }

    private static JPanel createDayCell(String text, boolean isToday, int eventCount) {
        JPanel cell = new JPanel(new GridBagLayout());
        cell.setBackground(PANEL_BACKGROUND);
        cell.setBorder(BorderFactory.createLineBorder(isToday ? ACCENT : BORDER));

        JLabel label = new JLabel(text);
        label.setFont(isToday ? new Font("SansSerif", Font.BOLD, 13) : BODY_FONT);
        label.setForeground(TEXT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        cell.add(label, gbc);

        if (!text.isBlank() && eventCount > 0) {
            JLabel countLabel = new JLabel(eventCount + (eventCount == 1 ? " event" : " events"));
            countLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
            countLabel.setForeground(new Color(34, 139, 34));
            gbc.gridy = 1;
            gbc.insets = new Insets(2, 0, 0, 0);
            cell.add(countLabel, gbc);
        }

        return cell;
    }

    private static JPanel createCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(PANEL_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 16, 16, 16)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SECTION_FONT);
        titleLabel.setForeground(TEXT);
        card.add(titleLabel, BorderLayout.NORTH);
        return card;
    }

    private static JLabel createSmallLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SECTION_FONT);
        label.setForeground(TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel createRoleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(TEXT);
        return label;
    }

    private static JLabel createMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(MUTED_TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel createBodyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel createFullWidthLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
        return label;
    }

    private static JScrollPane createStableScrollPane(JList<?> list) {
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setMinimumSize(new Dimension(0, 0));
        return scrollPane;
    }

    private static class CalendarPanel extends JPanel {
        private final DateSelectionHandler handler;
        private YearMonth currentMonth = YearMonth.now();
        private MonthChangeHandler monthChangeHandler;
        private LocalDate selectedDate;
        private Map<LocalDate, Integer> eventCounts = new HashMap<>();
        private JPanel selectedCell;
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        private final JPanel grid = new JPanel(new GridLayout(7, 7, 6, 6));

        CalendarPanel(DateSelectionHandler handler) {
            super(new BorderLayout(0, 14));
            this.handler = handler;
            setOpaque(false);

            JPanel toolbar = new JPanel(new BorderLayout());
            toolbar.setOpaque(false);

            JButton previousButton = new JButton("<");
            JButton nextButton = new JButton(">");
            monthLabel.setFont(SECTION_FONT);
            monthLabel.setForeground(TEXT);

            previousButton.addActionListener(e -> changeMonth(-1));
            nextButton.addActionListener(e -> changeMonth(1));

            toolbar.add(previousButton, BorderLayout.WEST);
            toolbar.add(monthLabel, BorderLayout.CENTER);
            toolbar.add(nextButton, BorderLayout.EAST);

            grid.setOpaque(false);

            add(toolbar, BorderLayout.NORTH);
            add(grid, BorderLayout.CENTER);

            renderMonth();
        }

        void setMonthChangeHandler(MonthChangeHandler monthChangeHandler) {
            this.monthChangeHandler = monthChangeHandler;
        }

        YearMonth getCurrentMonth() {
            return currentMonth;
        }

        void showEventCounts(Map<LocalDate, Integer> eventCounts) {
            this.eventCounts = new HashMap<>(eventCounts);
            renderMonth();
        }

        private void changeMonth(int amount) {
            currentMonth = currentMonth.plusMonths(amount);
            selectedCell = null;
            renderMonth();
            if (monthChangeHandler != null) {
                monthChangeHandler.monthChanged(currentMonth);
            }
        }

        private void renderMonth() {
            monthLabel.setText(currentMonth.format(MONTH_FORMATTER));
            grid.removeAll();
            selectedCell = null;

            addWeekdayHeaders();
            addDayCells();

            grid.revalidate();
            grid.repaint();
        }

        private void addWeekdayHeaders() {
            String[] weekdays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (String weekday : weekdays) {
                JLabel label = new JLabel(weekday, SwingConstants.CENTER);
                label.setFont(new Font("SansSerif", Font.BOLD, 12));
                label.setForeground(MUTED_TEXT);
                grid.add(label);
            }
        }

        private void addDayCells() {
            LocalDate firstDay = currentMonth.atDay(1);
            int firstDayOffset = firstDay.getDayOfWeek().getValue() % 7;
            int daysInMonth = currentMonth.lengthOfMonth();
            LocalDate today = LocalDate.now();

            for (int cell = 0; cell < 42; cell++) {
                int day = cell - firstDayOffset + 1;
                if (day < 1 || day > daysInMonth) {
                    grid.add(createDayCell("", false, 0));
                } else {
                    LocalDate date = currentMonth.atDay(day);
                    JPanel dayCell = createDayCell(String.valueOf(day), date.equals(today),
                            eventCounts.getOrDefault(date, 0));
                    dayCell.putClientProperty("date", date);
                    dayCell.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                    dayCell.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            selectDay(dayCell, date);
                            handler.dateSelected(date);
                        }
                    });
                    if (date.equals(selectedDate)) {
                        applySelectedStyle(dayCell);
                        selectedCell = dayCell;
                    }
                    grid.add(dayCell);
                }
            }
        }

        private void selectDay(JPanel dayCell, LocalDate date) {
            if (selectedCell != null) {
                resetDayCell(selectedCell);
            }

            selectedCell = dayCell;
            selectedDate = date;
            applySelectedStyle(dayCell);
        }

        private void applySelectedStyle(JPanel dayCell) {
            dayCell.setBackground(SELECTED_DAY_BACKGROUND);
            dayCell.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, BORDER, Color.GRAY));
            dayCell.repaint();
        }

        private void resetDayCell(JPanel dayCell) {
            Object dateValue = dayCell.getClientProperty("date");
            boolean isToday = dateValue instanceof LocalDate && dateValue.equals(LocalDate.now());

            dayCell.setBackground(PANEL_BACKGROUND);
            dayCell.setBorder(BorderFactory.createLineBorder(isToday ? ACCENT : BORDER));
            dayCell.repaint();
        }
    }

    private static class EventInfoPanel extends JPanel {
        private final DefaultListModel<String> eventListModel = new DefaultListModel<>();
        private final JList<String> eventList = new JList<>(eventListModel);
        private final JPanel songsPanel = new JPanel();
        private final JScrollPane songsScrollPane = new JScrollPane(songsPanel);
        private final JPanel selectedEventPanel = new JPanel(new BorderLayout(8, 0));
        private final JButton editTopicButton = new JButton("Edit Topic");
        private final JButton editSongsButton = new JButton("Edit Songs");
        private final JButton deleteEventButton = new JButton("Delete Event");
        private final JButton addEventButton = new JButton("Add Event");
        private List<DayEvent> currentEvents = new ArrayList<>();
        private AddEventHandler addEventHandler;
        private DeleteEventHandler deleteEventHandler;
        private EditTopicHandler editTopicHandler;
        private EditSongsHandler editSongsHandler;
        private SelectedEventHandler selectedEventHandler;

        EventInfoPanel() {
            super(new BorderLayout(0, 10));
            setBackground(PANEL_BACKGROUND);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(14, 16, 16, 16)
            ));
            setMinimumSize(new Dimension(420, 260));
            setPreferredSize(new Dimension(430, 300));

            JLabel titleLabel = new JLabel("Event / Topic / Song");
            titleLabel.setFont(SECTION_FONT);
            titleLabel.setForeground(TEXT);

            addEventButton.setEnabled(false);
            addEventButton.addActionListener(e -> {
                if (addEventHandler != null) {
                    addEventHandler.addEvent();
                }
            });

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(titleLabel, BorderLayout.WEST);
            header.add(addEventButton, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            eventList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            eventList.setFont(BODY_FONT);
            eventList.setPrototypeCellValue("00:00 Event title preview");
            eventList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    updateSelectedEventPanel();
                }
            });

            editTopicButton.setFont(new Font("SansSerif", Font.PLAIN, 11));
            editTopicButton.setMargin(new Insets(2, 8, 2, 8));
            editTopicButton.addActionListener(e -> {
                DayEvent event = getSelectedEvent();
                if (event != null && editTopicHandler != null) {
                    editTopicHandler.editTopic(event);
                }
            });
            editSongsButton.setFont(new Font("SansSerif", Font.PLAIN, 11));
            editSongsButton.setMargin(new Insets(2, 8, 2, 8));
            editSongsButton.addActionListener(e -> {
                DayEvent event = getSelectedEvent();
                if (event != null && editSongsHandler != null) {
                    editSongsHandler.editSongs(event);
                }
            });
            deleteEventButton.setFont(new Font("SansSerif", Font.PLAIN, 11));
            deleteEventButton.setMargin(new Insets(2, 8, 2, 8));
            deleteEventButton.addActionListener(e -> {
                DayEvent event = getSelectedEvent();
                if (event != null && deleteEventHandler != null) {
                    deleteEventHandler.deleteEvent(event);
                }
            });

            JPanel selectedButtonPanel = new JPanel(new GridLayout(1, 3, 6, 0));
            selectedButtonPanel.setOpaque(false);
            selectedButtonPanel.add(editTopicButton);
            selectedButtonPanel.add(editSongsButton);
            selectedButtonPanel.add(deleteEventButton);

            selectedEventPanel.setBackground(new Color(248, 249, 250));
            selectedEventPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
            selectedEventPanel.add(selectedButtonPanel, BorderLayout.WEST);
            selectedEventPanel.setVisible(false);

            songsPanel.setOpaque(false);
            songsPanel.setLayout(new BoxLayout(songsPanel, BoxLayout.Y_AXIS));
            songsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            songsScrollPane.setBorder(BorderFactory.createEmptyBorder());
            songsScrollPane.setOpaque(false);
            songsScrollPane.getViewport().setOpaque(false);
            songsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            songsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            songsScrollPane.setPreferredSize(new Dimension(0, 72));
            songsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
            songsScrollPane.setVisible(false);

            JPanel lowerPanel = new JPanel();
            lowerPanel.setOpaque(false);
            lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
            selectedEventPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            songsScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            lowerPanel.add(selectedEventPanel);
            lowerPanel.add(songsScrollPane);

            JPanel body = new JPanel(new BorderLayout(0, 12));
            body.setOpaque(false);
            body.add(createStableScrollPane(eventList), BorderLayout.CENTER);
            body.add(lowerPanel, BorderLayout.SOUTH);
            add(body, BorderLayout.CENTER);

            showEmpty();
        }

        void setAddEventHandler(AddEventHandler addEventHandler) {
            this.addEventHandler = addEventHandler;
        }

        void setDeleteEventHandler(DeleteEventHandler deleteEventHandler) {
            this.deleteEventHandler = deleteEventHandler;
        }

        void setEditTopicHandler(EditTopicHandler editTopicHandler) {
            this.editTopicHandler = editTopicHandler;
        }

        void setEditSongsHandler(EditSongsHandler editSongsHandler) {
            this.editSongsHandler = editSongsHandler;
        }

        void setSelectedEventHandler(SelectedEventHandler selectedEventHandler) {
            this.selectedEventHandler = selectedEventHandler;
        }

        void setSelectedDate(LocalDate date) {
            addEventButton.setEnabled(date != null);
        }

        void showEmpty() {
            currentEvents = new ArrayList<>();
            eventListModel.clear();
            songsPanel.removeAll();
            songsScrollPane.setVisible(false);
            selectedEventPanel.setVisible(false);
            notifySelectedEvent(null);
            refresh();
        }

        void showLoading(LocalDate date) {
            currentEvents = new ArrayList<>();
            eventListModel.clear();
            eventListModel.addElement("Loading " + date + "...");
            songsPanel.removeAll();
            songsScrollPane.setVisible(false);
            selectedEventPanel.setVisible(false);
            notifySelectedEvent(null);
            refresh();
        }

        void showSaving(LocalDate date) {
            currentEvents = new ArrayList<>();
            eventListModel.clear();
            eventListModel.addElement("Adding event to " + date + "...");
            songsPanel.removeAll();
            songsScrollPane.setVisible(false);
            selectedEventPanel.setVisible(false);
            notifySelectedEvent(null);
            refresh();
        }

        void showSavingTopic(LocalDate date) {
            currentEvents = new ArrayList<>();
            eventListModel.clear();
            eventListModel.addElement("Updating topic on " + date + "...");
            songsPanel.removeAll();
            songsScrollPane.setVisible(false);
            selectedEventPanel.setVisible(false);
            notifySelectedEvent(null);
            refresh();
        }

        void showSavingSongs(LocalDate date) {
            currentEvents = new ArrayList<>();
            eventListModel.clear();
            eventListModel.addElement("Updating songs on " + date + "...");
            songsPanel.removeAll();
            songsScrollPane.setVisible(false);
            selectedEventPanel.setVisible(false);
            notifySelectedEvent(null);
            refresh();
        }

        void showDeleting(LocalDate date) {
            currentEvents = new ArrayList<>();
            eventListModel.clear();
            eventListModel.addElement("Deleting event from " + date + "...");
            songsPanel.removeAll();
            songsScrollPane.setVisible(false);
            selectedEventPanel.setVisible(false);
            notifySelectedEvent(null);
            refresh();
        }

        void showEvents(LocalDate date, List<DayEvent> events) {
            currentEvents = new ArrayList<>(events);
            eventListModel.clear();
            songsPanel.removeAll();
            songsScrollPane.setVisible(false);
            selectedEventPanel.setVisible(false);
            notifySelectedEvent(null);

            if (events.isEmpty()) {
                eventListModel.addElement("No events on " + date);
            } else {
                for (DayEvent event : events) {
                    eventListModel.addElement(formatEventListLine(event));
                }
            }

            refresh();
        }

        void selectEventByTitle(String title) {
            for (int i = 0; i < currentEvents.size(); i++) {
                if (currentEvents.get(i).title.equals(title)) {
                    eventList.setSelectedIndex(i);
                    eventList.ensureIndexIsVisible(i);
                    updateSelectedEventPanel();
                    return;
                }
            }
        }

        void showError(String message) {
            currentEvents = new ArrayList<>();
            eventListModel.clear();
            eventListModel.addElement("Failed to load events.");
            songsPanel.removeAll();
            songsPanel.add(createMutedLabel(message));
            songsScrollPane.setVisible(true);
            selectedEventPanel.setVisible(false);
            notifySelectedEvent(null);
            refresh();
        }

        private void showSelectedEventDetails(DayEvent event) {
            songsPanel.removeAll();
            songsScrollPane.setVisible(true);
            songsPanel.add(createFullWidthLabel("Topic", SECTION_FONT, TEXT));
            if (event.topic.isBlank()) {
                songsPanel.add(createFullWidthLabel("No topic added.", BODY_FONT, MUTED_TEXT));
            } else {
                songsPanel.add(createFullWidthLabel(shorten(event.topic, 52), BODY_FONT, MUTED_TEXT));
            }

            songsPanel.add(createFullWidthLabel("Songs", SECTION_FONT, TEXT));
            if (event.songs.isBlank()) {
                songsPanel.add(createFullWidthLabel("No songs added.", BODY_FONT, MUTED_TEXT));
                return;
            }

            for (String song : event.songs.split("; ")) {
                if (!song.isBlank()) {
                    songsPanel.add(createFullWidthLabel(shorten(song, 52), BODY_FONT, MUTED_TEXT));
                }
            }
        }

        private String formatEventListLine(DayEvent event) {
            String line = event.time + " " + event.title;
            if (!event.topic.isBlank()) {
                line += " - " + event.topic;
            }
            return shorten(line, 60);
        }

        private void refresh() {
            selectedEventPanel.revalidate();
            selectedEventPanel.repaint();
            songsPanel.revalidate();
            songsPanel.repaint();
        }

        private void updateSelectedEventPanel() {
            DayEvent event = getSelectedEvent();
            if (event == null) {
                selectedEventPanel.setVisible(false);
                songsPanel.removeAll();
                songsScrollPane.setVisible(false);
                notifySelectedEvent(null);
                refresh();
                return;
            }

            selectedEventPanel.setVisible(true);
            showSelectedEventDetails(event);
            notifySelectedEvent(event);
            refresh();
        }

        private void notifySelectedEvent(DayEvent event) {
            if (selectedEventHandler != null) {
                selectedEventHandler.eventSelected(event);
            }
        }

        private DayEvent getSelectedEvent() {
            int index = eventList.getSelectedIndex();
            if (index < 0 || index >= currentEvents.size()) {
                return null;
            }
            return currentEvents.get(index);
        }
    }

    private static class SongsEditorPanel extends JPanel {
        private final DefaultListModel<SongDraft> songListModel = new DefaultListModel<>();
        private final JList<SongDraft> songList = new JList<>(songListModel);

        SongsEditorPanel(String songs) {
            super(new BorderLayout(0, 8));
            setPreferredSize(new Dimension(360, 260));

            JButton deleteButton = new JButton("Delete");
            JButton addSongButton = new JButton("Add Song");

            deleteButton.addActionListener(e -> {
                int selectedIndex = songList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    songListModel.remove(selectedIndex);
                }
            });
            addSongButton.addActionListener(e -> {
                SongDraft song = new SongDraft("song name", "author");
                songListModel.addElement(song);
                int index = songListModel.size() - 1;
                songList.setSelectedIndex(index);
                songList.ensureIndexIsVisible(index);
            });

            JPanel actionPanel = new JPanel(new GridLayout(1, 2, 8, 0));
            actionPanel.add(deleteButton);
            actionPanel.add(addSongButton);
            add(actionPanel, BorderLayout.NORTH);

            songList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            songList.setFont(BODY_FONT);
            songList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        editSelectedSong();
                    }
                }
            });

            for (SongDraft song : parseSongs(songs)) {
                songListModel.addElement(song);
            }

            add(createStableScrollPane(songList), BorderLayout.CENTER);
        }

        List<SongDraft> getSongs() {
            List<SongDraft> songs = new ArrayList<>();
            Map<String, SongDraft> seenByName = new LinkedHashMap<>();
            for (int i = 0; i < songListModel.size(); i++) {
                SongDraft song = songListModel.getElementAt(i).normalized();
                if (song.name.isBlank()) {
                    throw new IllegalArgumentException("Song name cannot be empty.");
                }
                if (song.name.contains("|") || song.author.contains("|")) {
                    throw new IllegalArgumentException("Song text cannot contain '|'.");
                }
                String key = song.name.toLowerCase(Locale.ENGLISH);
                if (seenByName.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate song name: " + song.name);
                }
                seenByName.put(key, song);
                songs.add(song);
            }
            return songs;
        }

        private void editSelectedSong() {
            int index = songList.getSelectedIndex();
            if (index < 0) {
                return;
            }

            SongDraft current = songListModel.getElementAt(index);
            javax.swing.JTextField songField = new javax.swing.JTextField(current.toDisplayText(), 26);
            int result = JOptionPane.showConfirmDialog(
                    this,
                    songField,
                    "Edit Song",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            songListModel.set(index, parseSongDraft(songField.getText()));
        }
    }

    private static class TeamInfoPanel extends JPanel {
        private final JPanel body = new JPanel(new GridLayout(0, 2, 8, 4));
        private final JButton addMemberButton = new JButton("Add Member");
        private final JButton deleteMemberButton = new JButton("Delete");
        private DayEvent selectedEvent;
        private String selectedMemberName;
        private AddMemberHandler addMemberHandler;
        private DeleteMemberHandler deleteMemberHandler;

        TeamInfoPanel() {
            super(new BorderLayout(0, 10));
            setBackground(PANEL_BACKGROUND);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(14, 16, 16, 16)
            ));
            setMinimumSize(new Dimension(420, 160));
            setPreferredSize(new Dimension(430, 200));

            JLabel titleLabel = new JLabel("Who Plays What");
            titleLabel.setFont(SECTION_FONT);
            titleLabel.setForeground(TEXT);

            addMemberButton.setEnabled(false);
            addMemberButton.addActionListener(e -> {
                if (selectedEvent != null && addMemberHandler != null) {
                    addMemberHandler.addMember(selectedEvent);
                }
            });
            deleteMemberButton.setEnabled(false);
            deleteMemberButton.addActionListener(e -> {
                if (selectedEvent != null && selectedMemberName != null && deleteMemberHandler != null) {
                    deleteMemberHandler.deleteMember(selectedEvent, selectedMemberName);
                }
            });

            JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 8, 0));
            buttonPanel.setOpaque(false);
            buttonPanel.add(addMemberButton);
            buttonPanel.add(deleteMemberButton);

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(titleLabel, BorderLayout.WEST);
            header.add(buttonPanel, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            body.setOpaque(false);
            JScrollPane memberScrollPane = new JScrollPane(body);
            memberScrollPane.setBorder(BorderFactory.createEmptyBorder());
            memberScrollPane.setOpaque(false);
            memberScrollPane.getViewport().setOpaque(false);
            memberScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            memberScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            memberScrollPane.setMinimumSize(new Dimension(0, 0));
            add(memberScrollPane, BorderLayout.CENTER);
            showEmpty();
        }

        void setAddMemberHandler(AddMemberHandler addMemberHandler) {
            this.addMemberHandler = addMemberHandler;
        }

        void setDeleteMemberHandler(DeleteMemberHandler deleteMemberHandler) {
            this.deleteMemberHandler = deleteMemberHandler;
        }

        void showEmpty() {
            selectedEvent = null;
            selectedMemberName = null;
            addMemberButton.setEnabled(false);
            deleteMemberButton.setEnabled(false);
            body.removeAll();
            refresh();
        }

        void showLoading() {
            body.removeAll();
            body.add(createMutedLabel("Loading..."));
            refresh();
        }

        void showError(String message) {
            body.removeAll();
            body.add(createMutedLabel(message));
            refresh();
        }

        void showEvent(DayEvent event) {
            selectedEvent = event;
            selectedMemberName = null;
            addMemberButton.setEnabled(event != null);
            deleteMemberButton.setEnabled(false);
            body.removeAll();
            if (event == null) {
                refresh();
                return;
            }

            if (event.team.isBlank()) {
                body.add(createMutedLabel("No team members added."));
                refresh();
                return;
            }

            for (String memberRole : event.team.split("; ")) {
                if (!memberRole.isBlank()) {
                    String[] parts = memberRole.split(" - ", 2);
                    String memberName = parts[0];
                    String role = parts.length == 2 ? parts[1] : "No position";
                    JLabel memberLabel = createRoleLabel(shorten(memberName, 28));
                    JLabel roleLabel = createMutedLabel(shorten(role, 42));
                    JPanel memberCell = createMemberCell(memberLabel, memberName);
                    JPanel roleCell = createMemberCell(roleLabel, memberName);
                    body.add(memberCell);
                    body.add(roleCell);
                }
            }
            refresh();
        }

        DayEvent getSelectedEvent() {
            return selectedEvent;
        }

        private JPanel createMemberCell(JLabel label, String memberName) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setOpaque(true);
            cell.setBackground(PANEL_BACKGROUND);
            cell.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            cell.setMinimumSize(new Dimension(0, 30));
            cell.setPreferredSize(new Dimension(0, 30));
            cell.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            cell.putClientProperty("memberName", memberName);
            label.setVerticalAlignment(SwingConstants.CENTER);
            cell.add(label, BorderLayout.CENTER);
            cell.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectMember(memberName);
                }
            });
            return cell;
        }

        private void selectMember(String memberName) {
            selectedMemberName = memberName;
            deleteMemberButton.setEnabled(true);

            for (Component component : body.getComponents()) {
                if (component instanceof JPanel) {
                    component.setBackground(PANEL_BACKGROUND);
                }
            }

            for (Component component : body.getComponents()) {
                if (component instanceof JPanel) {
                    JPanel cell = (JPanel) component;
                    if (memberName.equals(cell.getClientProperty("memberName"))) {
                        cell.setBackground(SELECTED_DAY_BACKGROUND);
                    }
                }
            }
            refresh();
        }

        private void refresh() {
            body.revalidate();
            body.repaint();
        }
    }

    private static class ActivityInfoPanel extends JPanel {
        private final DefaultListModel<String> activityListModel = new DefaultListModel<>();
        private final JList<String> activityList = new JList<>(activityListModel);

        ActivityInfoPanel() {
            super(new BorderLayout(0, 10));
            setBackground(PANEL_BACKGROUND);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(14, 16, 16, 16)
            ));
            setMinimumSize(new Dimension(0, 130));

            JLabel titleLabel = new JLabel("User Activity");
            titleLabel.setFont(SECTION_FONT);
            titleLabel.setForeground(TEXT);
            add(titleLabel, BorderLayout.NORTH);

            activityList.setFont(BODY_FONT);
            activityList.setPrototypeCellValue("2026-06-25T10:00:00,Anna,ADD_EVENT");
            add(createStableScrollPane(activityList), BorderLayout.CENTER);
        }

        void showActivities(List<String> activities) {
            activityListModel.clear();
            for (int i = activities.size() - 1; i >= 0; i--) {
                String activity = activities.get(i);
                activityListModel.addElement(formatActivityForDisplay(activity));
            }
        }

        void showError(String message) {
            activityListModel.clear();
            activityListModel.addElement("Failed to load user activity: " + message);
        }
    }

    private static void refreshUserActivity(ActivityInfoPanel activityInfoPanel, ClientConnection connection) {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws IOException {
                return fetchUserActivities(connection);
            }

            @Override
            protected void done() {
                try {
                    activityInfoPanel.showActivities(get());
                } catch (Exception ex) {
                    activityInfoPanel.showError(ex.getMessage());
                }
            }
        }.execute();
    }

    private static void subscribeToServerChanges(
            EventInfoPanel eventInfoPanel,
            TeamInfoPanel teamInfoPanel,
            ActivityInfoPanel activityInfoPanel,
            CalendarPanel calendarPanel,
            ClientConnection connection,
            LocalDate[] selectedDate
    ) {
        connection.addChangeListener(() -> SwingUtilities.invokeLater(() -> {
            refreshCalendarCounts(calendarPanel, connection);
            refreshUserActivity(activityInfoPanel, connection);
            if (selectedDate[0] != null) {
                refreshSelectedDate(eventInfoPanel, teamInfoPanel, activityInfoPanel,
                        connection, selectedDate[0]);
            }
        }));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                connection.sendCommandForResponse("SUBSCRIBE_CHANGES");
                return null;
            }
        }.execute();
    }

    private static void refreshSelectedDate(
            EventInfoPanel eventInfoPanel,
            TeamInfoPanel teamInfoPanel,
            ActivityInfoPanel activityInfoPanel,
            ClientConnection connection,
            LocalDate selectedDate
    ) {
        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws IOException {
                String response = connection.sendCommandForResponse("LIST_DAY_DETAIL|" + selectedDate);
                List<DayEvent> events = parseDayDetailResponse(response);
                List<String> activities = fetchUserActivities(connection);
                return new DashboardData(events, activities);
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    DayEvent selectedEvent = teamInfoPanel.getSelectedEvent();
                    String selectedTitle = selectedEvent == null ? null : selectedEvent.title;

                    eventInfoPanel.showEvents(selectedDate, data.events);
                    activityInfoPanel.showActivities(data.activities);

                    if (selectedTitle != null) {
                        DayEvent updatedEvent = findEventByTitle(data.events, selectedTitle);
                        eventInfoPanel.selectEventByTitle(selectedTitle);
                        teamInfoPanel.showEvent(updatedEvent);
                    } else {
                        teamInfoPanel.showEmpty();
                    }
                } catch (Exception ex) {
                    eventInfoPanel.showError(ex.getMessage());
                    teamInfoPanel.showEmpty();
                }
            }
        }.execute();
    }

    private static void refreshCalendarCounts(CalendarPanel calendarPanel, ClientConnection connection) {
        YearMonth month = calendarPanel.getCurrentMonth();
        new SwingWorker<Map<LocalDate, Integer>, Void>() {
            @Override
            protected Map<LocalDate, Integer> doInBackground() throws IOException {
                return parseMonthCountsResponse(
                        connection.sendCommandForResponse("LIST_MONTH_COUNTS|" + month)
                );
            }

            @Override
            protected void done() {
                try {
                    if (month.equals(calendarPanel.getCurrentMonth())) {
                        calendarPanel.showEventCounts(get());
                    }
                } catch (Exception ignored) {
                    calendarPanel.showEventCounts(new HashMap<>());
                }
            }
        }.execute();
    }

    private static List<DayEvent> parseDayDetailResponse(String response) {
        List<DayEvent> events = new ArrayList<>();
        if (response == null || !response.startsWith("DAY_DETAIL")) {
            return events;
        }

        String[] records = response.split("\\|");
        for (int i = 1; i < records.length; i++) {
            String[] fields = records[i].split(",", -1);
            if (fields.length == 6) {
                events.add(new DayEvent(
                        decode(fields[0]),
                        decode(fields[1]),
                        decode(fields[2]),
                        decode(fields[3]),
                        decode(fields[4]),
                        decode(fields[5])
                ));
            } else if (fields.length == 5) {
                events.add(new DayEvent(
                        decode(fields[0]),
                        decode(fields[1]),
                        decode(fields[2]),
                        "",
                        decode(fields[3]),
                        decode(fields[4])
                ));
            }
        }
        return events;
    }

    private static Map<LocalDate, Integer> parseMonthCountsResponse(String response) {
        Map<LocalDate, Integer> counts = new HashMap<>();
        if (response == null || !response.startsWith("MONTH_COUNTS")) {
            return counts;
        }

        String[] records = response.split("\\|");
        for (int i = 1; i < records.length; i++) {
            String[] fields = records[i].split(",", -1);
            if (fields.length == 2) {
                counts.put(LocalDate.parse(decode(fields[0])), Integer.parseInt(fields[1]));
            }
        }
        return counts;
    }

    private static DayEvent findEventByTitle(List<DayEvent> events, String title) {
        for (DayEvent event : events) {
            if (event.title.equals(title)) {
                return event;
            }
        }
        return null;
    }

    private static List<SongDraft> parseSongs(String songs) {
        List<SongDraft> parsedSongs = new ArrayList<>();
        if (songs == null || songs.isBlank()) {
            return parsedSongs;
        }

        for (String songText : songs.split("; ")) {
            if (!songText.isBlank()) {
                parsedSongs.add(parseSongDraft(songText));
            }
        }
        return parsedSongs;
    }

    private static SongDraft parseSongDraft(String songText) {
        String text = songText == null ? "" : songText.trim();
        int dashIndex = text.indexOf(" - ");
        if (dashIndex >= 0) {
            return new SongDraft(text.substring(0, dashIndex), text.substring(dashIndex + 3));
        }

        int byIndex = text.lastIndexOf(" by ");
        if (byIndex >= 0) {
            return new SongDraft(text.substring(0, byIndex), text.substring(byIndex + 4));
        }
        if (text.endsWith(" by")) {
            return new SongDraft(text.substring(0, text.length() - 3), "");
        }

        return new SongDraft(text, "");
    }

    private static List<String> fetchUserActivities(ClientConnection connection) throws IOException {
        return parseUserActivityResponse(connection.sendCommandForResponse("LIST_USER_ACTIVITY"));
    }

    private static List<String> parseUserActivityResponse(String response) {
        List<String> activities = new ArrayList<>();
        if (response == null || !response.startsWith("USER_ACTIVITY")) {
            return activities;
        }

        String[] records = response.split("\\|");
        for (int i = 1; i < records.length; i++) {
            activities.add(decode(records[i]));
        }
        return activities;
    }

    private static String formatActivityForDisplay(String activity) {
        String[] parts = activity.split(",", 3);
        if (parts.length < 3) {
            return shorten(activity, 110);
        }

        String time = parts[0];
        String username = parts[1];
        String command = parts[2];
        String[] commandParts = command.split("\\|");
        String action = commandParts[0];
        String formattedTime = formatActivityTime(time);

        if ("ADD_EVENT".equals(action) && commandParts.length >= 4) {
            return shorten(formattedTime + " - [" + username + "] added event [" + commandParts[3]
                    + "] on [" + commandParts[1] + "]", 110);
        }
        if ("REMOVE_EVENT".equals(action) && commandParts.length >= 2) {
            return shorten(formattedTime + " - [" + username + "] removed event [" + commandParts[1] + "]", 110);
        }
        if ("EDIT_TOPIC".equals(action) && commandParts.length >= 3) {
            String topic = decodeActivityValue(commandParts[2]);
            return shorten(formattedTime + " - [" + username + "] edited topic for [" + commandParts[1]
                    + "] to [" + topic + "]", 110);
        }
        if ("ADD_SONG".equals(action) && commandParts.length >= 3) {
            if (commandParts.length >= 5) {
                return shorten(formattedTime + " - [" + username + "] added song [" + commandParts[2]
                        + "] from [" + commandParts[1] + "] on [" + commandParts[4] + "]", 110);
            }
            return shorten(formattedTime + " - [" + username + "] added song [" + commandParts[2] + "]", 110);
        }
        if ("REMOVE_SONG".equals(action) && commandParts.length >= 3) {
            if (commandParts.length >= 4) {
                return shorten(formattedTime + " - [" + username + "] removed song [" + commandParts[2]
                        + "] from [" + commandParts[1] + "] on [" + commandParts[3] + "]", 110);
            }
            return shorten(formattedTime + " - [" + username + "] removed song [" + commandParts[2] + "]", 110);
        }
        if ("ADD_MEMBER".equals(action) && commandParts.length >= 3) {
            if (commandParts.length >= 4) {
                return shorten(formattedTime + " - [" + username + "] added member [" + commandParts[2]
                        + "] to [" + commandParts[1] + "] as [" + commandParts[3] + "]", 110);
            }
            return shorten(formattedTime + " - [" + username + "] added member [" + commandParts[2] + "]", 110);
        }
        if ("REMOVE_MEMBER".equals(action) && commandParts.length >= 3) {
            return shorten(formattedTime + " - [" + username + "] removed member [" + commandParts[2] + "]", 110);
        }

        return shorten(formattedTime + " - [" + username + "] " + action, 110);
    }

    private static String formatActivityTime(String time) {
        try {
            return LocalDateTime.parse(time).format(ACTIVITY_TIME_FORMATTER);
        } catch (Exception ex) {
            return time;
        }
    }

    private static String decodeActivityValue(String value) {
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return value;
        }
    }

    private static String shorten(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        if (maxLength <= 3) {
            return text.substring(0, maxLength);
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private interface DateSelectionHandler {
        void dateSelected(LocalDate date);
    }

    private interface MonthChangeHandler {
        void monthChanged(YearMonth month);
    }

    private interface AddEventHandler {
        void addEvent();
    }

    private interface DeleteEventHandler {
        void deleteEvent(DayEvent event);
    }

    private interface EditTopicHandler {
        void editTopic(DayEvent event);
    }

    private interface EditSongsHandler {
        void editSongs(DayEvent event);
    }

    private interface SelectedEventHandler {
        void eventSelected(DayEvent event);
    }

    private interface AddMemberHandler {
        void addMember(DayEvent event);
    }

    private interface DeleteMemberHandler {
        void deleteMember(DayEvent event, String memberName);
    }

    private static class SongDraft {
        private final String name;
        private final String author;

        SongDraft(String name, String author) {
            this.name = name == null ? "" : name;
            this.author = author == null ? "" : author;
        }

        SongDraft normalized() {
            return new SongDraft(name.trim(), author.trim());
        }

        String toDisplayText() {
            return name + " - " + author;
        }

        @Override
        public String toString() {
            return toDisplayText();
        }
    }

    private static class DayEvent {
        private final String date;
        private final String time;
        private final String title;
        private final String topic;
        private final String songs;
        private final String team;

        DayEvent(String date, String time, String title, String topic, String songs, String team) {
            this.date = date;
            this.time = time;
            this.title = title;
            this.topic = topic;
            this.songs = songs;
            this.team = team;
        }
    }

    private static class DashboardData {
        private final List<DayEvent> events;
        private final List<String> activities;

        DashboardData(List<DayEvent> events, List<String> activities) {
            this.events = events;
            this.activities = activities;
        }
    }

    public ClientConnection getConnection() {
        return connection;
    }

    private void closeConnection() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (IOException ignored) {
        }
    }
}
