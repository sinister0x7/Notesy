package net.intensecorp.notesy.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.textfield.TextInputEditText;

import net.intensecorp.notesy.R;
import net.intensecorp.notesy.adapters.NotesAdapter;
import net.intensecorp.notesy.database.NotesDatabase;
import net.intensecorp.notesy.entities.Note;
import net.intensecorp.notesy.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements NotesListener {
    private static final int REQUEST_CODE_ADD_NOTE = 1;
    private static final int REQUEST_CODE_UPDATE_NOTE = 2;
    private static final int REQUEST_CODE_SHOW_NOTES = 3;

    private RecyclerView mNotesRecyclerView;
    private List<Note> mNoteList;
    private NotesAdapter mNotesAdapter;
    private int mNoteClickedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ImageView addNoteButton = findViewById(R.id.imageView_add_note);
        TextInputEditText searchInputField = findViewById(R.id.textInputEditText_search_notes);
        mNotesRecyclerView = findViewById(R.id.recyclerView_notes);

        mNotesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        mNoteList = new ArrayList<>();
        mNotesAdapter = new NotesAdapter(mNoteList, this);
        mNotesRecyclerView.setAdapter(mNotesAdapter);

        getNotes(REQUEST_CODE_SHOW_NOTES, false);

        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getApplicationContext(), CreateNoteActivity.class), REQUEST_CODE_ADD_NOTE);
            }
        });

        searchInputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mNotesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mNoteList.size() != 0) {
                    mNotesAdapter.searchNotes(s.toString());
                }
            }
        });
    }

    private void getNotes(final int requestCode, final boolean isNoteDeleted) {
        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {
            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase.getDatabase(getApplicationContext()).noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                if (requestCode == REQUEST_CODE_SHOW_NOTES) {
                    mNoteList.addAll(notes);
                    mNotesAdapter.notifyDataSetChanged();
                } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                    mNoteList.add(0, notes.get(0));
                    mNotesAdapter.notifyItemInserted(0);
                    mNotesRecyclerView.smoothScrollToPosition(0);
                } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    mNoteList.remove(mNoteClickedPosition);
                    if (isNoteDeleted) {
                        mNotesAdapter.notifyItemRemoved(mNoteClickedPosition);
                    } else {
                        mNoteList.add(mNoteClickedPosition, notes.get(mNoteClickedPosition));
                        mNotesAdapter.notifyItemChanged(mNoteClickedPosition);
                    }
                }
            }
        }
        new GetNotesTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            getNotes(REQUEST_CODE_ADD_NOTE, false);
        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if (data != null) {
                getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
            }
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        mNoteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
    }
}
