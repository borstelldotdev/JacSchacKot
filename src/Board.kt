enum class Player(val value: Byte) {
    WHITE(1),
    BLACK(-1),
    NONE(0)
}

enum class PieceType(val value: Short) {
    PAWN(100),
    KNIGHT(300),
    BISHOP(320),
    ROOK(500),
    QUEEN(900),
    KING(16384); // To prevent overflows

    companion object {
        fun fromValue(v: Int) = entries.firstOrNull { it.value == v.toShort() } ?: error("Unknown piece value: $v")
    }
}

@JvmInline
value class Piece(val raw: Short) {

    val owner: Player get() = when {
        raw > 0 -> Player.WHITE
        raw < 0 -> Player.BLACK
        else    -> Player.NONE
    }

    val pieceValue:
            Int get() = kotlin.math.abs(raw.toInt())

    val isEmpty:
            Boolean get() = raw == 0.toShort()

    override fun toString():
            String = "Piece(${owner} ${PieceType.fromValue(pieceValue)})"

    companion object {
        // Sort-of constructor
        fun of(type: PieceType, owner: Player): Piece {
            return Piece((type.value * owner.value).toShort())
        }

        // Empty square
        val EMPTY = Piece(0)
    }
}

@JvmInline
value class Square(val raw: Int) {
    // x011  x110
    // from  to

    val x:
            Int get() = raw and 0b0000_0111
    val y:
            Int get() = (raw and 0b0111_0000) shr 4

    fun offset(xOffset: Int, yOffset: Int): Square {
        return Square((raw + xOffset + (yOffset shl 4)) and 0b0111_0111)
    }

    companion object {
        fun of(x: Int, y: Int) = (y shl 4) + x
    }
}


@JvmInline
value class BoardMeta(val raw: ULong) {

    // B0-1: Full move
    // B2: Half move (caps at 50)
    // B3: to move (0000 0010: white, 0000 0001 black),
    // B4: en passant square
    // B5: Castling (WK, WQ, BK, BQ)
    // B6: White king position
    // B7: Black king position

    val fullMove:
            Short get() = (raw and 0x00_00_00_00_00_00_FF_FFuL).toShort()

    val halfMove:
            Byte get() = ((raw and 0x00_00_00_00_00_FF_00_00uL) shr 16).toByte()

    val toMove:
            Player get() = when {
                (raw and 0x00_00_00_00_02_00_00_00u) != 0uL -> Player.WHITE
                raw and 0x00_00_00_00_01_00_00_00u != 0uL -> Player.BLACK
                else -> Player.NONE
            }

    val enPassantSquare:
            Square get() = Square(((raw and 0x00_00_00_FF_00_00_00_00uL) shr 32).toInt())

    val whiteKingsideCastle:
            Boolean get() = (raw and 0x00_00_01_00_00_00_00_00uL) != 0uL
    val whiteQueensideCastle:
            Boolean get() = (raw and 0x00_00_02_00_00_00_00_00uL) != 0uL
    val blackKingsideCastle:
            Boolean get() = (raw and 0x00_00_04_00_00_00_00_00uL) != 0uL
    val blackQueensideCastle:
            Boolean get() = (raw and 0x00_00_08_00_00_00_00_00uL) != 0uL

    val whiteKing:
            Square get() = Square(((raw and 0x00_FF_00_00_00_00_00_00uL) shr 48).toInt())

    val blackKing:
            Square get() = Square(((raw and 0xFF_00_00_00_00_00_00_00uL) shr 56).toInt())

    companion object {
        // WTF
        fun of(
            fullMove: Short, halfMove: Byte, toMove: Player, enPassantSquare: Square,
            whiteKingsideCastle: Boolean, whiteQueensideCastle: Boolean,
            blackKingsideCastle: Boolean, blackQueensideCastle: Boolean,
            whiteKing: Square, blackKing: Square
        ): BoardMeta {
            return BoardMeta(fullMove.toULong() or (halfMove.toULong() shl 16) or when (toMove) {
                Player.WHITE -> 0x00_00_01_00_00_00_00_00uL
                Player.BLACK -> 0x00_00_02_00_00_00_00_00uL
                else -> 0uL
            } or (enPassantSquare.raw.toULong() shl 32) or
                    when (whiteKingsideCastle) {true -> 0x00_00_01_00_00_00_00_00uL; else -> 0uL} or
                    when (whiteQueensideCastle) {true -> 0x00_00_02_00_00_00_00_00uL; else -> 0uL} or
                    when (blackKingsideCastle) {true -> 0x00_00_04_00_00_00_00_00uL; else -> 0uL} or
                    when (blackQueensideCastle) {true -> 0x00_00_08_00_00_00_00_00uL; else -> 0uL} or
                whiteKing.raw.toULong() shl 48 or blackKing.raw.toULong() shl 56
            )
        }
    }
}



class Board(val data: ShortArray, var toMove: Player) {

}