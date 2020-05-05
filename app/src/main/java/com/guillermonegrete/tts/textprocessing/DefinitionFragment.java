package com.guillermonegrete.tts.textprocessing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.guillermonegrete.tts.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DefinitionFragment extends Fragment {


    private static WiktionaryAdapter mAdapter;

    public static DefinitionFragment newInstance(WiktionaryAdapter adapter){
        DefinitionFragment fragment = new DefinitionFragment();

        // TODO Instead of sending the adapter, send a list of parcelable items
        mAdapter = adapter;

        return fragment;

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View fragment_layout = inflater.inflate(R.layout.fragment_process_definition, container, false);

        RecyclerView mlistView =  fragment_layout.findViewById(R.id.recycler_view_wiki);
        mlistView.setAdapter(mAdapter);
        mlistView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return fragment_layout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter = null;
    }
}
