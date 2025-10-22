package LR2T.buyer;

import java.util.Date;

class BookInfo {
    String title;
    int maxPrice;
    Date deadline;

    public String getTitle() { return title; }
    public void setTitle(String title) {this.title = title;}

    public int getMaxPrice() { return maxPrice; }
    public void setMaxPrice(int maxPrice) {this.maxPrice = maxPrice;}

    public Date getDeadline() { return deadline;}
    public void setDeadline(Date deadline) {this.deadline = deadline;}
}
