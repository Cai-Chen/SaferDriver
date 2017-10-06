package unimelb.comp90018.saferdriver;

/**
 * Created by nikaixuan on 28/9/17.
 */

public class RoadInfo {

        @com.google.gson.annotations.SerializedName("id")
        public String mId;

        @com.google.gson.annotations.SerializedName("road")
        private String mRoad;

        @com.google.gson.annotations.SerializedName("description")
        private String mDescription;

        public RoadInfo() {

        }

        public RoadInfo(String mRoadName, String mDescription)
        {
            this.mRoad = mRoadName;
            this.mDescription = mDescription;
        }

        public String getmRoad()
        {
            return mRoad;
        }

        public void setmRoad(String mRoad)
        {
            this.mRoad = mRoad;
        }

        public String getmDescription()
        {
            return mDescription;
        }

        public void setmDescription(String mDescription)
        {
            this.mDescription = mDescription;
        }
}


