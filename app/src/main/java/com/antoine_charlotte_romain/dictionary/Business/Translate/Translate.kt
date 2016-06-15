package com.dicosaure.Business.Translate

import com.dicosaure.Business.Word.Word

/**
 * Created by dineen on 15/06/2016.
 */
abstract class Translate {

    var wordTo : Word
    var wordFrom : Word

    constructor(wordTo: Word, wordFrom: Word) {
        this.wordFrom = wordFrom
        this.wordTo = wordTo
    }

    abstract fun save()
    abstract fun delete()
    abstract fun modify()

}
