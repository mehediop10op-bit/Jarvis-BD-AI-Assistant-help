package com.example

import com.example.data.CommandHistoryEntity
import com.example.data.ReceptionistMessageEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTestSuite {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testCommandHistoryEntity() {
        val entry = CommandHistoryEntity(
            commandText = "আমার ফোনে কত চার্জ আছে?",
            responseText = "আপনার ফোনের ব্যাটারি ৭৫ শতাংশ।",
            commandType = "VOICE",
            isSuccess = true
        )
        assertEquals("আমার ফোনে কত চার্জ আছে?", entry.commandText)
        assertTrue(entry.isSuccess)
    }

    @Test
    fun testReceptionistMessageEntity() {
        val message = ReceptionistMessageEntity(
            callerName = "বাবা",
            callerNumber = "+880 1711-223344",
            transcript = "Jarvis: আসসালামু আলাইকুম।\nকলার: আমি বাবা বলছি।",
            messagePreview = "বাসায় ফেরার পথে ফল নিয়ে এসো।",
            callbackRequested = true,
            callbackTime = "বিকেল ৫টা",
            isRead = false,
            mode = "MODE_B"
        )
        assertEquals("বাবা", message.callerName)
        assertTrue(message.callbackRequested)
        assertEquals("বিকেল ৫টা", message.callbackTime)
    }

    @Test
    fun testPermissionTypes() {
        val mic = com.example.manager.PermissionType.MICROPHONE
        val sms = com.example.manager.PermissionType.SMS
        val contacts = com.example.manager.PermissionType.CONTACTS
        assertTrue(mic.isEssential)
        assertTrue(contacts.isEssential)
        assertTrue(sms.isEssential)
        assertEquals(android.Manifest.permission.RECORD_AUDIO, mic.permissionString)
        assertEquals(android.Manifest.permission.SEND_SMS, sms.permissionString)
        assertEquals(3, com.example.manager.PermissionManager.ESSENTIAL_PERMISSIONS.size)
        assertTrue(com.example.manager.PermissionManager.ESSENTIAL_PERMISSIONS.contains(android.Manifest.permission.RECORD_AUDIO))
        assertTrue(com.example.manager.PermissionManager.ESSENTIAL_PERMISSIONS.contains(android.Manifest.permission.READ_CONTACTS))
        assertTrue(com.example.manager.PermissionManager.ESSENTIAL_PERMISSIONS.contains(android.Manifest.permission.SEND_SMS))
    }
}
