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

        fun fromChar(ch: Char): Piece {
            return when (ch) {
                'P' -> of(PieceType.PAWN, Player.WHITE)
                'N' -> of(PieceType.KNIGHT, Player.WHITE)
                'B' -> of(PieceType.BISHOP, Player.WHITE)
                'R' -> of(PieceType.ROOK, Player.WHITE)
                'Q' -> of(PieceType.QUEEN, Player.WHITE)
                'K' -> of(PieceType.KING, Player.WHITE)

                'p' -> of(PieceType.PAWN, Player.BLACK)
                'n' -> of(PieceType.KNIGHT, Player.BLACK)
                'b' -> of(PieceType.BISHOP, Player.BLACK)
                'r' -> of(PieceType.ROOK, Player.BLACK)
                'q' -> of(PieceType.QUEEN, Player.BLACK)
                'k' -> of(PieceType.KING, Player.BLACK)

                else -> EMPTY
            }
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
        fun of(x: Int, y: Int): Square = Square((y shl 4) + x)

        fun fromString(string: String): Square = Square.of(
            x = "ABCDEFGI".indexOf(string[0].lowercase()),
            y = "87654321".indexOf(string[0])
        )
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
            Byte get() = ((raw and 0x00_00_00_00_00_FF_00_00uL) shr 16).coerceIn(0uL, 50uL).toByte()

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
        private fun Boolean.toULong(mask: ULong) = if (this) mask else 0uL

        fun of(
            fullMove: Short,
            halfMove: Byte,
            toMove: Player,
            enPassantSquare: Square,
            whiteKingsideCastle: Boolean,
            whiteQueensideCastle: Boolean,
            blackKingsideCastle: Boolean,
            blackQueensideCastle: Boolean,
            whiteKing: Square,
            blackKing: Square
        ): BoardMeta {
            val toMoveBits = when (toMove) {
                Player.WHITE -> 0x01uL
                Player.BLACK -> 0x02uL
                else -> 0x00uL
            }
            return BoardMeta(
                fullMove.toULong()                                          // B0–1 : full move
                        or (halfMove.toULong() shl 16)                             // B1   : half move  (if re-laid out)
                        or (toMoveBits shl 24)                                    // B3   : to move
                        or (enPassantSquare.raw.toULong() shl 32)                 // B4   : en passant
                        or whiteKingsideCastle.toULong(0x01uL shl 40)      // B2   : WK castle
                        or whiteQueensideCastle.toULong(0x02uL shl 40)     // B2   : WQ castle
                        or blackKingsideCastle.toULong(0x04uL shl 40)      // B2   : BK castle
                        or blackQueensideCastle.toULong(0x08uL shl 40)     // B2   : BQ castle
                        or (whiteKing.raw.toULong() shl 48)                       // B6   : white king
                        or (blackKing.raw.toULong() shl 56)                       // B7   : black king
            )
        }

        fun fromFen(fen: String, whiteKing: Square, blackKing: Square): BoardMeta {
            val (toPlay, castlingAvailability, enPassantSquare, halfMoveClock, fullMoveClock) =
                fen.split(' ', limit = 5)

            val toPlayPlayer = when (toPlay.lowercase()) {
                "w" -> Player.WHITE
                "b" -> Player.BLACK
                else -> Player.NONE
            }

            var whiteKingsideCastle = false
            var whiteQueensideCastle = false
            var blackKingsideCastle = false
            var blackQueensideCastle = false

            for (ch in castlingAvailability.toCharArray()) {
                when (ch) {
                    'K' -> whiteKingsideCastle = true
                    'Q' -> whiteQueensideCastle = true
                    'k' -> blackKingsideCastle = true
                    'q' -> blackQueensideCastle = true
                }
            }

            return BoardMeta.of(
                fullMove = fullMoveClock.toShort(),
                halfMove = halfMoveClock.toByte(),
                toMove = toPlayPlayer,
                enPassantSquare = Square.fromString(enPassantSquare),
                whiteKingsideCastle = whiteKingsideCastle,
                whiteQueensideCastle = whiteQueensideCastle,
                blackKingsideCastle = blackKingsideCastle,
                blackQueensideCastle = blackQueensideCastle,
                whiteKing = whiteKing,
                blackKing = blackKing
            )
        }
    }
}

@JvmInline
value class BoardData(val raw: ShortArray) {
    companion object {
        fun fromFen(fen: String): Triple<BoardData, Square, Square> {
            val new = BoardData(ShortArray(64))
            var whiteKing: Square = Square(0)
            var blackKing: Square = Square(0)
            val ranks = fen.split('/') // Single quotes: Char
            for (rank in 0..7) {
                var file = 0
                for (ch in ranks[rank].toCharArray()) {
                    if (ch.isDigit()) {
                        file += ch.toString().toInt() // not pretty
                    } else {
                        new[file, rank] = Piece.fromChar(ch).raw
                        when (ch) {
                            'K' -> whiteKing = Square.of(file, rank)
                            'k' -> blackKing = Square.of(file, rank)
                        }
                        file += 1
                    }
                }
            }


            return Triple(new, whiteKing, blackKing)
        }
    }

    operator fun get(x: Int, y: Int): Short {
        if (!(x in 0..7 && y in 0..7)) {
            return 0
        }

        return raw[x + (y * 8)]
    }

    operator fun get(square: Square): Short {
        return get(square.x, square.y)
    }

    operator fun set(x: Int, y: Int, value: Short) {
        if (!(x in 0..7 && y in 0..7)) {
            return
        }

        raw[x + (y * 8)] = value
    }

    operator fun set(square: Square, value: Short) {
        return set(square.x, square.y, value)
    }

     fun atUnsafe(x: Int, y: Int): Short {
        return raw[x + (y * 8)]
    }

    fun at(x: Int, y: Int): Short {
        return get(x, y)
    }

    fun at(square: Square): Short {
        return get(square.x, square.y)
    }
}


class Board(val data: BoardData, val meta: BoardMeta) {
    companion object {
        fun fromFen(fen: String): Board {
            val (boardStr, metaStr) = fen.split(' ', limit = 2)
            val (boardData, whiteKing, blackKing) = BoardData.fromFen(boardStr)
            val boardMeta = BoardMeta.fromFen(metaStr, whiteKing, blackKing)
            return Board(boardData, boardMeta)
        }

        fun startingPosition(): Board {
            return fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        }
    }

    override fun toString(): String {
        // TODO: fix
        val str = "Board\n  A B C D E F G H I\n\n"

        return  str
    }
}