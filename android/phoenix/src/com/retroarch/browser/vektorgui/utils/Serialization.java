package com.retroarch.browser.vektorgui.utils;

public class Serialization {
	public static class gameClass {
		private String gameTitle = null;
		private String coverURL = null;
		private String gameDescription = "";
		private String gameYear = null;

		public gameClass(String title, String url, String description,
				String year) {
			gameTitle = title;
			coverURL = url;
			gameDescription = description;
			gameYear = year;
		}

		public void setTitle(String title) {
			this.gameTitle = title;
		}

		public String getTitle() {
			return gameTitle;
		}

		public void setURL(String url) {
			this.coverURL = url;
		}

		public String getURL() {
			return coverURL;
		}
		
		public void setYear(String year){
			this.gameYear=year;
		}

		public String getYear(){
			return this.gameYear;
		}
		public void setDescription(String description){
			this.gameDescription=description;
		}
		public String getDescription(){
			return this.gameDescription;
		}
		public void clear() {
			coverURL = null;
			gameTitle = null;
			gameDescription = "";
			gameYear = null;
		}
	}
}
