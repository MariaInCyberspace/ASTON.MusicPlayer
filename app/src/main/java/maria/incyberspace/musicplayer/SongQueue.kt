package maria.incyberspace.musicplayer


interface SongQueue {

    fun <T> getSongPosition(index: Int, songsSource: List<T>) : Int {
        if (index in 0..songsSource.lastIndex) {
            return index
        } else if (index < 0) {
            return songsSource.lastIndex
        } else if (index > songsSource.lastIndex) {
            return 0
        } else {
            return index
        }
    }
}