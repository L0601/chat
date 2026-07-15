package com.example.lunadesk.data

import java.util.UUID

fun interface IdGenerator {
    fun nextId(): String
}

class UuidIdGenerator : IdGenerator {
    override fun nextId(): String = UUID.randomUUID().toString()
}
