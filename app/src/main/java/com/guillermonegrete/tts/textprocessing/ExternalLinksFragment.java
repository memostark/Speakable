package com.guillermonegrete.tts.textprocessing;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.db.ExternalLink;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ExternalLinksFragment extends Fragment {

    private Context mContext;

    private String wordExtra;

    private List<ExternalLink> links;

    private RecyclerView recyclerView;
    private DefaultWebBrowser defaultWebBrowser;

    private static final String WORD_TEXT = "word_text";
    private static final String LINKS_LIST = "links";

    static ExternalLinksFragment newInstance(String word, ArrayList<ExternalLink> links){
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
        if (args != null) {
            wordExtra = args.getString(WORD_TEXT);
            links = args.getParcelableArrayList(LINKS_LIST);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.external_links_grid, container, false);
        recyclerView = fragment_layout.findViewById(R.id.external_links_recycle);
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));

        defaultWebBrowser = getDefaultWebBrowser();
        setExternalLinks(links);
        return fragment_layout;
    }

    void setExternalLinks(List<ExternalLink> links){

        // External links can be updated when fragment is not attached, in that case update args
        // Otherwise update adapter normally
        if(isAdded()){
            ExternalLinksAdapter adapter = new ExternalLinksAdapter(wordExtra, links,
                    url -> startActivity(defaultWebBrowser.intentForUrl(requireContext(), url)));
            recyclerView.setAdapter(adapter);
        }else{
            Bundle args = getArguments();
            if (args != null) {
                args.putParcelableArrayList(LINKS_LIST, new ArrayList<>(links));
            }
        }
    }

    private DefaultWebBrowser getDefaultWebBrowser(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String preference = preferences.getString(DefaultWebBrowser.PREFERENCE_KEY, "");
        if (preference == null) preference = "";
        return DefaultWebBrowser.Companion.get(preference);
    }

}
