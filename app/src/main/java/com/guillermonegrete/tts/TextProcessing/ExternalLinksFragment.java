package com.guillermonegrete.tts.TextProcessing;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.db.ExternalLink;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ExternalLinksFragment extends Fragment {

    private Context mContext;

    private String wordExtra;

    private List<ExternalLink> links;

    private static final String WORD_TEXT = "word_text";
    private static final String LINKS_LIST = "links";


    private final static String ReversoConjugationBaseURL = "http://conjugator.reverso.net/conjugation-hebrew-verb-";

    public static ExternalLinksFragment newInstance(String word, ArrayList<ExternalLink> links){
        ExternalLinksFragment fragment = new ExternalLinksFragment();

        Bundle args = new Bundle();
        args.putString(WORD_TEXT, word);
        args.putParcelableArrayList(LINKS_LIST, links);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        mContext = context;
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        wordExtra = args.getString(WORD_TEXT);
        links = args.getParcelableArrayList(LINKS_LIST);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.external_links_grid, container, false);
        /*fragment_layout.findViewById(R.id.external_link_btn_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(mContext, Uri.parse(
                        ReversoConjugationBaseURL + wordExtra  + ".html"
                ));
            }
        });*/
        RecyclerView recyclerView = fragment_layout.findViewById(R.id.external_links_recycle);

        ExternalLinksAdapter adapter = new ExternalLinksAdapter(mContext, wordExtra, links);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));
        return fragment_layout;
    }
}
