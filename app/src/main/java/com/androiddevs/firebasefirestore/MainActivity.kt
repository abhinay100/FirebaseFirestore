package com.androiddevs.firebasefirestore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {

    private val personCollectionRef = Firebase.firestore.collection("persons")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUploadData.setOnClickListener {
            val person = getOldPerson()
            savePerson(person)
        }

       // subscribeToRealTimeUpdates()

        btnRetrieveData.setOnClickListener {
            retrievePersons()
        }

        btnUpdatePerson.setOnClickListener {
            val oldPerson = getOldPerson()
            val newPersonMap = getNewPersonMap()
            updatePerson(oldPerson, newPersonMap)
        }

        btnDeletePerson.setOnClickListener {
            val person = getOldPerson()
            deletePerson(person)
        }

        btnBatchWrite.setOnClickListener {
            changeName("8U4mw7S1JdfA9JOjwifV", "Elon", "Musk")
        }
    }

    private fun getOldPerson(): Person {
        val firstName = etFirstName.text.toString()
        val lastName = etLastName.text.toString()
        val age = etAge.text.toString().toInt()
        return  Person(firstName, lastName, age)
    }

    private fun getNewPersonMap(): Map<String, Any> {
        val firstName = etNewFirstName.text.toString()
        val lastName = etNewLastName.text.toString()
        val age = etAge.text.toString()

        val map = mutableMapOf<String, Any>()
        if(firstName.isNotEmpty()) {
            map["firstName"] = firstName
        }
        if(lastName.isNotEmpty()) {
            map["lastName"] = lastName
        }
        if(age.isNotEmpty()) {
            map["age"] = age.toInt()
        }
        return map
    }

    private fun deletePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {

        val personQuery = personCollectionRef
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age)
            .get()
            .await()

        if(personQuery.documents.isNotEmpty()) {
            for(document in personQuery) {
                try {
                    personCollectionRef.document(document.id).delete().await()
                   /* personCollectionRef.document(document.id).update(mapOf(
                        "firstName" to FieldValue.delete()
                    ))*/
                }catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }

        }else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No Person Matched the query", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun updatePerson(person: Person, newPersomMap: Map<String, Any>) = CoroutineScope(Dispatchers.IO).launch {

        val personQuery = personCollectionRef
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age)
            .get()
            .await()

        if(personQuery.documents.isNotEmpty()) {
            for(document in personQuery) {
                try {
                    personCollectionRef.document(document.id).set(
                        newPersomMap,
                        SetOptions.merge()
                    ).await()
                }catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }

        }else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No Person Matched the query", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun changeName(
        personId: String,
        newFirstName: String,
        newLastName:String
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase.firestore.runBatch { batch ->
                val personRef = personCollectionRef.document(personId)
                batch.update(personRef, "firstName", newFirstName)
                batch.update(personRef, "lastName", newLastName)
            }.await()
        }catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun subscribeToRealTimeUpdates() {
        personCollectionRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException.let {
                Toast.makeText(this@MainActivity, it?.message, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            querySnapshot?.let {
                val sb = StringBuilder()
                for (document in it) {
                    val person = document.toObject<Person>()
                    sb.append("$person\n")
                }
                tvPersons.text = sb.toString()
            }

        }
    }

    private fun retrievePersons() = CoroutineScope(Dispatchers.IO).launch {

        val fromAge = etFrom.text.toString().toInt()
        val toAge = etTo.text.toString().toInt()
        try {
            val querySnapshot = personCollectionRef
                .whereGreaterThan("age", fromAge)
                .whereLessThan("age", toAge)
                .orderBy("age")
                .get()
                .await()
            val sb = StringBuilder()
            for(document in  querySnapshot.documents) {
                val person = document.toObject<Person>()
                sb.append("$person\n")
            }
            withContext(Dispatchers.Main) {
                tvPersons.text = sb.toString()
            }
        }catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun savePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        try {
            personCollectionRef.add(person).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Successfully saved data.", Toast.LENGTH_LONG).show()
            }
        } catch(e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
