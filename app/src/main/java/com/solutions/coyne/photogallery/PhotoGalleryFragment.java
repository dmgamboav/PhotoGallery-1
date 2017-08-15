package com.solutions.coyne.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Patrick Coyne on 8/14/2017.
 */

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView photoRecyclerView;
    private List<GalleryItem> items = new ArrayList<>();
    private int previousTotal = 0;
    private boolean loading = true;
    private int visibleThreshold = 5;
    int firstVisibleItem, visibleItemCount, totalItemCount;
    int page =1;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute(page);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        photoRecyclerView = (RecyclerView)view.findViewById(R.id.photo_recycler_view);
        final GridLayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 3);
        photoRecyclerView.setLayoutManager(mLayoutManager);
        photoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                visibleItemCount = photoRecyclerView.getChildCount();
                totalItemCount = mLayoutManager.getItemCount();
                firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();

                if (loading) {
                    if (totalItemCount > previousTotal) {
                        loading = false;
                        previousTotal = totalItemCount;
                    }
                }
                if (!loading && (totalItemCount - visibleItemCount)
                        <= (firstVisibleItem + visibleThreshold)) {
                    // End has been reached

                    Log.i("Yaeye!", "end called");

                    page++;
                    // Do something

                    loading = true;
                    new FetchItemsTask().execute(page);
                }
            }
        });

        setupAdapter();

        return view;
    }

    private void setupAdapter(){
        if(isAdded()){
            photoRecyclerView.setAdapter(new PhotoAdapter(items));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder{
        private TextView titleTextView;

        public PhotoHolder(View itemView) {
            super(itemView);
            titleTextView = (TextView)itemView;
        }

        public void bindGalleryItem(GalleryItem item){
            titleTextView.setText(item.toString());
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> galleryItemList;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            galleryItemList = galleryItems;
        }
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = galleryItemList.get(position);
            holder.bindGalleryItem(galleryItem);
        }

        @Override
        public int getItemCount() {
            return galleryItemList.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>>{
        @Override
        protected List<GalleryItem> doInBackground(Integer... integers) {
            return new FlickrFetchr().fetchItems(integers[0]);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> returnItems) {
            items.addAll(returnItems);
            setupAdapter();
        }
    }
}
