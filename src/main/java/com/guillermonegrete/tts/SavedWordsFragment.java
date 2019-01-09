package com.guillermonegrete.tts;


import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.guillermonegrete.tts.SavedWords.SavedWordListAdapter;
import com.guillermonegrete.tts.SavedWords.WordsViewModel;
import com.guillermonegrete.tts.db.Words;
import com.guillermonegrete.tts.db.WordsDAO;
import com.guillermonegrete.tts.db.WordsDatabase;

import java.util.List;

public class SavedWordsFragment extends Fragment {

    private SavedWordListAdapter wordListAdapter;
    private WordsViewModel wordsViewModel;
    private RecyclerView mRecyclerView;
    private Context context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        wordListAdapter = new SavedWordListAdapter(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.fragment_saved_words, container, false);

        mRecyclerView = fragment_layout.findViewById(R.id.recyclerview_saved_words);
        mRecyclerView.setAdapter(wordListAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        setUpItemTouchHelper();

        fragment_layout.findViewById(R.id.new_word_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSaveDialog();
            }
        });
        return fragment_layout;
    }

    private void initData(){
        wordsViewModel = ViewModelProviders.of(this).get(WordsViewModel.class);
        wordsViewModel.getWordsList().observe(this, new Observer<List<Words>>() {
            @Override
            public void onChanged(@Nullable List<Words> movies) {
                wordListAdapter.setWordsList(movies);
            }
        });
    }

    public void removeData() {
        if (wordListAdapter != null) {
            wordsViewModel.deleteAll();
        }
    }

    private void showSaveDialog() {
        DialogFragment dialogFragment;
        dialogFragment = SaveWordDialogFragment.newInstance(null, null, null);
        dialogFragment.show(getActivity().getSupportFragmentManager(), "New word");
    }

    private void setUpItemTouchHelper(){
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            Drawable deleteIcon ;
            Drawable backgroundColor;

            boolean initiated;
            private int intrinsicWidth;
            private int intrinsicHeight;

            private void init() {
                backgroundColor = new ColorDrawable(Color.RED);
                deleteIcon  = ContextCompat.getDrawable(context, R.drawable.ic_delete_black_24dp);
                deleteIcon .setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                // xMarkMargin = (int) MainActivity.this.getResources().getDimension(R.dimen.ic_clear_margin);
                intrinsicWidth = deleteIcon.getIntrinsicWidth();
                intrinsicHeight = deleteIcon.getIntrinsicHeight();
                initiated = true;
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                TextView saved_word_text = viewHolder.itemView.findViewById(R.id.saved_word_text);
                String word_text = saved_word_text.getText().toString();
                Toast.makeText(context, "Swiped word: " + word_text, Toast.LENGTH_SHORT).show();

                WordsDAO wordsDAO = WordsDatabase.getDatabase(context).wordsDAO();
                wordsDAO.deleteWord(word_text);

            }

            @Override
            public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (!initiated) {
                    init();
                }

                View itemView = viewHolder.itemView;
                int itemHeight = itemView.getBottom() - itemView.getTop();

                // Draw the red delete background
                Drawable backgroundColor = new ColorDrawable(Color.RED);
                backgroundColor.setBounds(
                        itemView.getRight() + (int)dX,
                        itemView.getTop(),
                        itemView.getRight(),
                        itemView.getBottom()
                );
                backgroundColor.draw(canvas);

                // Calculate position of delete icon
                int iconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                int iconMargin = (itemHeight - intrinsicHeight) / 2;
                int iconLeft = itemView.getRight() - iconMargin - intrinsicWidth;
                int iconRight = itemView.getRight() - iconMargin;
                int iconBottom = iconTop + intrinsicHeight;

                // Draw the delete icon
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(canvas);

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    public class DividerItemDecoration extends RecyclerView.ItemDecoration {


        private final int[] ATTRS = new int[]{
                android.R.attr.listDivider
        };

        public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;

        public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;

        private Drawable mDivider;

        private int mOrientation;

        public DividerItemDecoration(Context context, int orientation) {
            final TypedArray a = context.obtainStyledAttributes(ATTRS);
            mDivider = a.getDrawable(0);
            a.recycle();
            setOrientation(orientation);
        }

        public void setOrientation(int orientation) {
            if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
                throw new IllegalArgumentException("invalid orientation");
            }
            mOrientation = orientation;
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent) {
            if (mOrientation == VERTICAL_LIST) {
                drawVertical(c, parent);
            } else {
                drawHorizontal(c, parent);
            }
        }

        public void drawVertical(Canvas c, RecyclerView parent) {
            final int left = parent.getPaddingLeft();
            final int right = parent.getWidth() - parent.getPaddingRight();

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();
                final int top = child.getBottom() + params.bottomMargin;
                final int bottom = top + mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

        public void drawHorizontal(Canvas c, RecyclerView parent) {
            final int top = parent.getPaddingTop();
            final int bottom = parent.getHeight() - parent.getPaddingBottom();

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();
                final int left = child.getRight() + params.rightMargin;
                final int right = left + mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, int itemPosition, RecyclerView parent) {
            if (mOrientation == VERTICAL_LIST) {
                outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
            } else {
                outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
            }
        }
    }
}
