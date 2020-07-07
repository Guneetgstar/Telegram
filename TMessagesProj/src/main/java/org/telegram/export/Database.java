package org.telegram.export;

class Database {
    private static final Database ourInstance = new Database();

    static Database getInstance() {
        return ourInstance;
    }

    private Database() {
    }
}
