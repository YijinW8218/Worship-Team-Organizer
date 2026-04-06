package Model;

import java.time.LocalDateTime;

public class UserActivity {
    private final String userName;
    private final String actionType;
    private final LocalDateTime recordAt;

    public UserActivity(String userName, String actionType) {
        this.userName = userName;
        this.actionType = actionType;
        this.recordAt = LocalDateTime.now();
    }

    //getters
    public String getUserName() {return userName;}
    public String getActionType() {return actionType;}
    public LocalDateTime getRecordAt() {return recordAt;}

    @Override
    public String toString() {
        return "UserActivity{" +
                "userName='" + userName + '\'' +
                ", actionType='" + actionType + '\'' +
                ", recordAt=" + recordAt +
                '}';
    }
}
