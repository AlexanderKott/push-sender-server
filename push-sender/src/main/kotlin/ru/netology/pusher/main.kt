package ru.netology.pusher

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import ru.netology.pusher.constants.dbUrl
import ru.netology.pusher.constants.token
import java.awt.EventQueue
import java.awt.Font
import java.awt.GridLayout
import java.io.FileInputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.*


class SwingInterface(title: String) : JFrame() {

    init {
        createUI(title)

        val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(FileInputStream("fcm.json")))
                .setDatabaseUrl(dbUrl)
                .build()
        FirebaseApp.initializeApp(options)
    }


    private fun send(authorName: String, action: String, post: String, willBeNotified: Boolean, postID: String) {
        try {

            val message = Message.builder()
                    .putData("action", "$action")
                    .putData("content", """{
          "userId": 1,
          "userName": "$authorName",
          "postId": ${postID.toLong()} ,
          "postAuthor": "$post",
          "notification": "$willBeNotified"
            
           
        }""".trimIndent())
                    .setToken(token)
                    .build()



            FirebaseMessaging.getInstance().send(message)
            JOptionPane.showMessageDialog(this, "Готово");

        } catch (ex: Exception) {
            when (ex) {
                is ClassCastException, is NumberFormatException -> {
                    JOptionPane.showMessageDialog(this, "Проверьте поля. Поле post Id должно быть целым числом");
                }
                is FirebaseMessagingException -> {
                    val writer = StringWriter()
                    val printWriter = PrintWriter(writer)
                    ex.printStackTrace(printWriter)
                    printWriter.flush()
                    val stackTrace = writer.toString()
                    JOptionPane.showMessageDialog(this, "ERROR! \n $stackTrace")
                    ex.printStackTrace()
                }
            }

        }

    }


    private fun createUI(title: String) {
        setTitle(title)
        val sendBtn = JButton("Send")

        val name = JTextField(20)
        name.text = "Имя юзера"


        val postID = JTextField(20)
        postID.text = "post ID (int)"

        val petStrings = arrayOf<String?>("New", "like", "Share", "REMOVE")

        val actionsList: JComboBox<*> = JComboBox<Any?>(petStrings)

        val area1 = JTextArea("Текст сообщения для поста", 8, 10)
        area1.font = Font("Dialog", Font.PLAIN, 14)
        area1.tabSize = 10
        area1.lineWrap = true
        area1.wrapStyleWord = true


        val notificate = JCheckBox("Force activity launch", true)
        notificate.isSelected = false

        sendBtn.addActionListener {
            send(name.text, actionsList.getSelectedItem() as String,
                    area1.text, notificate.isSelected, postID.text)
        }

        crLayout(notificate, name, JScrollPane(area1), actionsList, postID, sendBtn)

        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setSize(800, 500)
        setLocationRelativeTo(null)
    }

    private fun crLayout(vararg arg: JComponent) {
        val grid = JPanel()
        val layout = GridLayout(2, 1, 5, 12)
        grid.setLayout(layout)
        for (i in arg) {
            grid.add(i)
        }
        contentPane.add(grid)
        pack()
    }


}


private fun createAndShowGUI() {
    val frame = SwingInterface("Netologia firebase send text")
    frame.isVisible = true
}

fun main() {
    EventQueue.invokeLater(::createAndShowGUI)
}



