package jiren.service.database

class ResultComparator(
    private var sourceList: MutableList<String>,
    private var targetList: MutableList<String>
) {
    fun compare(): MutableList<String>? {
        val diff = targetList.containsAll(sourceList)
        if (!diff) {
            sourceList.removeAll(targetList.toSet())
            return sourceList
        }
        return null
    }
}