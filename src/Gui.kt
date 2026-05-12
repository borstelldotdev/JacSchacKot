import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import javax.swing.JFrame
import javax.swing.JPanel

class DrawingPanel : JPanel() {
    companion object {
        // Colors can't be `const`
        val BACKGROUND_COLOR =              Color(48, 48, 48)
        val LIGHT_SQUARE_COLOR =            Color(255, 206, 158)
        val DARK_SQUARE_COLOR =             Color(209, 139, 71)
        val BITBOARD_INCLUDED_COLOR =       Color(54, 112, 207)
        val BITBOARD_NOT_INCLUDED_COLOR =   Color(191, 62, 55)
        val HIGHLIGHTED_SQUARE_COLOR =      Color(25, 224, 155)
        val SELECTED_SQUARE_MODIFIER =      Color(40, 30, 0)
        val SELECTED_SQUARE_OUTLINE =       Color(80, 80, 80)
        val FONT_COLOR =                    Color(255, 255, 2)
        val ARROW_COLOR =                   Color(255, 0, 0, 192)
    }

    init {
        background = BACKGROUND_COLOR
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        g.font = Font("Arial", Font.PLAIN, 32)

        val boardLeftX = (width / 2) - (Gui.SQUARE_SIZE * 4) + 32
        val boardTopY = (height / 2) - (Gui.SQUARE_SIZE * 4)
        val textTopY = boardTopY - 64

        for (x in 0..7) {
            for (y in 0..7) {
                g.color = if ((x + y) % 2 == 0) LIGHT_SQUARE_COLOR else DARK_SQUARE_COLOR
                g.fillRect(boardLeftX + x * Gui.SQUARE_SIZE, boardTopY + y * Gui.SQUARE_SIZE,
                    Gui.SQUARE_SIZE, Gui.SQUARE_SIZE)
            }
        }

        g.color = FONT_COLOR
        g.drawString("JacSchack", boardLeftX, textTopY)
    }
}


class Gui {

    companion object {
        const val SQUARE_SIZE = 60
    }


    init {
        val totalWidth: Int = SQUARE_SIZE * 12
        val totalHeight: Int = SQUARE_SIZE * 12

        val frame = JFrame("JacSchack")
        frame.apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            setSize(totalWidth, totalHeight)
            add(DrawingPanel())
            isVisible = true
        }
    }
}


