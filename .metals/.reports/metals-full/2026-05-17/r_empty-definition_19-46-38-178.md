error id: file:///C:/Users/User/Desktop/CMSC%20127%20NEW/la-festin/src/main/java/com/lefestin/Main.java:java/lang/Runtime#
file:///C:/Users/User/Desktop/CMSC%20127%20NEW/la-festin/src/main/java/com/lefestin/Main.java
empty definition using pc, found symbol in pc: java/lang/Runtime#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 434
uri: file:///C:/Users/User/Desktop/CMSC%20127%20NEW/la-festin/src/main/java/com/lefestin/Main.java
text:
```scala
package com.lefestin;

import javax.swing.SwingUtilities;

import com.lefestin.config.DBConnection;
import com.lefestin.ui.AppTheme;
import com.lefestin.ui.MainFrame;
import com.lefestin.ui.dialogs.LoginDialog;;

public class Main {
    public static void main(String[] args) {
        // Initializes overall app theme
        AppTheme.install(); 

        // Shut down the DB connection (upon Window close)
        Run@@time.getRuntime().addShutdownHook(new Thread(() -> {
            DBConnection.getInstance().close();
        }));

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            
            // Show login before making the main window visible
            LoginDialog login = new LoginDialog(frame);
            login.setVisible(true);

            // LoginDialog is modal — execution resumes here after
            // dispose() is called, which only happens on success.
            // If the user closed the dialog, System.exit(0) already ran.

            // Guarantees that ----- frame.getCurrentUser() is non-null!!!
            frame.setVisible(true);
        });

    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: java/lang/Runtime#