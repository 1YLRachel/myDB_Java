package com.db.sql.parser.listener;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * 执行时错误信息监听
 * @author Roy
 * @date 2022/11/5 21:58
 */
public class VerboseListener extends BaseErrorListener {
    /**
     * 错误处理函数
     * @param recognizer recognizer可以强转成Parser、CommonTokenStream等多个类型
     * @param offendingSymbol 我们将它强转成Token类型，可以获取它的开始与结束的字符位置
     * @param line 错误所在行数
     * @param charPositionInLine 所在列
     * @param msg 详细的错误信息
     * @param e
     */
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e) {
        System.out.println(msg);
    }
}
