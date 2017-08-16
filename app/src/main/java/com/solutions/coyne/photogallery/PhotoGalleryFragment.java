package com.solutions.coyne.photogallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.solutions.coyne.photogallery.Service.PollService;
import com.squareup.picasso.Picasso;

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
    private String query;

//    private ThumbnailDownloader<PhotoHolder> thumbnailDownloader;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        new FetchItemsTask().execute(String.valueOf(page), null);

//        Intent i = PollService.newIntent(getActivity());
//        getActivity().startService(i);
//        PollService.setServiceAlarm(getActivity(), true);

        Handler responseHandler = new Handler();
//        thumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
//        thumbnailDownloader.setThumbnailDownloaderListener(
//                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>(){
//                    @Override
//                    public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
//                        Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
//                        target.bindDrawable(drawable);
//                    }
//                });
//
//        thumbnailDownloader.start();
//        thumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView)searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "QueryTextSubmit: "+s);
                QueryPreferences.setStoredQuery(getActivity(), s);
                page = 1;
                query = s;
                updateItems(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if(PollService.isServiceAlarmOn(getActivity())){
            toggleItem.setTitle(R.string.stop_polling);
        }else{
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems(null);
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    new FetchItemsTask().execute(String.valueOf(page), query);
                }
            }
        });

        setupAdapter();

        return view;
    }

    private void setupAdapter(){
        if(isAdded()){
            photoRecyclerView.setAdapter(new PhotoAdapter(getActivity(), items));
        }
    }

    private void updateItems(String query){
        new FetchItemsTask().execute(String.valueOf(page), query);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        thumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        thumbnailDownloader.quit();
        Log.i(TAG, "Background thread Destroyed");
    }

    private class PhotoHolder extends RecyclerView.ViewHolder{
        ImageView itemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            itemImageView = (ImageView)itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable){
            itemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> galleryItemList;
        private Context context;

        public PhotoAdapter(Context context, List<GalleryItem> galleryItems){
            this.context = context;
            galleryItemList = galleryItems;
        }
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater= LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = galleryItemList.get(position);
//            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
//            holder.bindDrawable(placeholder);
//            thumbnailDownloader.queueThumbnail(holder, galleryItem.getmUrl());//Use Picasso or Glide for future releases
            Picasso.with(context)
                    .load(galleryItem.getmUrl())
//                    .placeholder(R.drawable.bill_up_close)
                    .error(R.drawable.bill_up_close)
                    .into(holder.itemImageView);
        }

        @Override
        public int getItemCount() {
            return galleryItemList.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<String, Void, List<GalleryItem>>{
        @Override
        protected List<GalleryItem> doInBackground(String... strings) {
            String query = null;
            if(strings.length >1){// adds a query parameter for search
                query = strings[1];
            }
            if(query == null){
                return new FlickrFetchr().fetchRecentPhotos(strings[0]);
            }else{
                return new FlickrFetchr().searchPhotos(query, strings[0]);
            }
        }
        @Override
        protected void onPostExecute(List<GalleryItem> returnItems) {
            if(page > 1) {
                items.addAll(returnItems);
            }else{
                items.clear();
                items.addAll(returnItems);
            }
            setupAdapter();
        }
    }
}
