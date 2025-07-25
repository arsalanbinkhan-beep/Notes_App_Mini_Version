package com.arsalankhan.notesappminiversion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arsalankhan.notesappminiversion.databinding.ActivityAddNoteBinding
import com.arsalankhan.notesappminiversion.databinding.ActivityMainBinding
import com.arsalankhan.notesappminiversion.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.*

data class Note(val title: String, val descripton: String, val timestamp: String) {
    fun toPrefString(): String = "$title|$descripton|$timestamp"

    companion object {
        fun fromPrefString(data: String): Note? {
            val parts = data.split("|")
            return if (parts.size == 3) Note(parts[0], parts[1], parts[2]) else null
        }
    }
}

class MainActivity : AppCompatActivity() {
    private val notesList = mutableListOf<Note>()
    private lateinit var adapter: NoteAdapter
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = NoteAdapter(notesList) { noteToDelete -> deleteNote(noteToDelete) }
        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewNotes.adapter = adapter

        binding.btnAddNote.setOnClickListener {
            startActivityForResult(Intent(this, AddNoteActivity::class.java), 1)
        }

        loadNotes()
    }

    private fun loadNotes() {
        notesList.clear()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val savedNotes = prefs.getStringSet("notes", setOf()) ?: setOf()
        savedNotes.mapNotNull { Note.fromPrefString(it) }.also {
            notesList.addAll(it)
        }
        adapter.notifyDataSetChanged()
    }

    private fun deleteNote(note: Note) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val current = prefs.getStringSet("notes", setOf())?.toMutableSet() ?: return
        current.remove(note.toPrefString())
        prefs.edit().putStringSet("notes", current).apply()
        loadNotes()
        Toast.makeText(this, "Note Deleted", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            loadNotes()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

class AddNoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddNoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSaveNote.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please Fill All Fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val note = Note(title, description, timestamp).toPrefString()
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val existing = prefs.getStringSet("notes", setOf())?.toMutableSet() ?: mutableSetOf()
            existing.add(note)
            prefs.edit().putStringSet("notes", existing).apply()

            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}

class ViewEditNoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddNoteBinding
    private var originalNoteString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra("title") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val timestamp = intent.getStringExtra("timestamp") ?: ""

        originalNoteString = "$title|$description|$timestamp"
        binding.etTitle.setText(title)
        binding.etDescription.setText(description)
        binding.btnSaveNote.text = "Update Note"

        binding.btnSaveNote.setOnClickListener {
            val newTitle = binding.etTitle.text.toString().trim()
            val newDescription = binding.etDescription.text.toString().trim()

            if (newTitle.isEmpty() || newDescription.isEmpty()) {
                Toast.makeText(this, "Please Fill All Fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val newNote = Note(newTitle, newDescription, newTimestamp).toPrefString()
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val set = prefs.getStringSet("notes", setOf())?.toMutableSet() ?: mutableSetOf()

            set.remove(originalNoteString)
            set.add(newNote)
            prefs.edit().putStringSet("notes", set).apply()

            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}

class NoteAdapter(
    private val notes: MutableList<Note>,
    private val onDelete: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.binding.tvTitle.text = note.title
        holder.binding.tvTimestamp.text = note.timestamp

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ViewEditNoteActivity::class.java).apply {
                putExtra("title", note.title)
                putExtra("description", note.descripton)
                putExtra("timestamp", note.timestamp)
            }
            (context as Activity).startActivityForResult(intent, 1)
        }

        holder.itemView.setOnLongClickListener {
            onDelete(note)
            true
        }
    }

    override fun getItemCount(): Int = notes.size
}
