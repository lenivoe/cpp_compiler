package com.parser

import com.common.CommonException
import com.lexer.token.Token

class ParserException(msg: String, token: Token)
    : CommonException("wrong token '${token.image}' at ${token.line}:${token.column}; $msg")