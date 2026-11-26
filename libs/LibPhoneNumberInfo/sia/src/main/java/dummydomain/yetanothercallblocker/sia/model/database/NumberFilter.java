package dummydomain.yetanothercallblocker.sia.model.database;

public interface NumberFilter {

    boolean keepPrefix(String prefix);

    boolean keepNumber(String number);

    boolean isDetailed();

}
