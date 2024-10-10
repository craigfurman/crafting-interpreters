defmodule Scanner do
  defguardp is_alpha(char)
            when is_integer(char) and (char in ?a..?z or char in ?A..?Z or char == ?_)

  defguardp is_digit(char)
            when is_integer(char) and char in ?0..?9

  defguardp is_alphanumeric(char)
            when is_alpha(char) or is_digit(char)

  def new(iodev) do
    stream = PeekableStream.new(IO.stream(iodev, 1))
    spawn_link(fn -> loop(stream, 1) end)
  end

  def next_token(pid), do: send_msg(pid, :next)
  def stop(pid), do: send(pid, :stop)

  defp loop(stream, line) do
    receive do
      {:next, caller} ->
        {token, stream, line} = scan(stream, line)
        send(caller, {:next, self(), token})
        loop(stream, line)
    end
  end

  defp scan(stream, line) do
    {char, stream} = PeekableStream.next(stream)

    case char do
      :eof -> {:eof, stream, line}
      _ -> scan_char(stream, line, to_codepoint(char))
    end
  end

  defp scan_char(stream, line, codepoint) do
    mkToken = fn type, codepoint ->
      %Token{type: type, lexeme: to_string([codepoint]), line: line}
    end

    case codepoint do
      ?( ->
        {mkToken.(:left_paren, codepoint), stream, line}

      ?) ->
        {mkToken.(:right_paren, codepoint), stream, line}

      ?{ ->
        {mkToken.(:left_brace, codepoint), stream, line}

      ?} ->
        {mkToken.(:right_brace, codepoint), stream, line}

      ?[ ->
        {mkToken.(:left_bracket, codepoint), stream, line}

      ?] ->
        {mkToken.(:right_bracket, codepoint), stream, line}

      ?, ->
        {mkToken.(:comma, codepoint), stream, line}

      ?. ->
        {mkToken.(:dot, codepoint), stream, line}

      ?- ->
        {mkToken.(:minus, codepoint), stream, line}

      ?+ ->
        {mkToken.(:plus, codepoint), stream, line}

      ?; ->
        {mkToken.(:semicolon, codepoint), stream, line}

      ?* ->
        {mkToken.(:star, codepoint), stream, line}

      ?! ->
        {peek, stream} = PeekableStream.peek(stream)

        case peek do
          "=" ->
            {_, stream} = PeekableStream.next(stream)
            {mkToken.(:bang_equal, [codepoint | String.to_charlist(peek)]), stream, line}

          _ ->
            {mkToken.(:bang, codepoint), stream, line}
        end

      ?= ->
        {peek, stream} = PeekableStream.peek(stream)

        case peek do
          "=" ->
            {_, stream} = PeekableStream.next(stream)
            {mkToken.(:equal_equal, [codepoint | String.to_charlist(peek)]), stream, line}

          _ ->
            {mkToken.(:equal, codepoint), stream, line}
        end

      ?< ->
        {peek, stream} = PeekableStream.peek(stream)

        case peek do
          "=" ->
            {_, stream} = PeekableStream.next(stream)
            {mkToken.(:less_equal, [codepoint | String.to_charlist(peek)]), stream, line}

          _ ->
            {mkToken.(:less, codepoint), stream, line}
        end

      ?> ->
        {peek, stream} = PeekableStream.peek(stream)

        case peek do
          "=" ->
            {_, stream} = PeekableStream.next(stream)
            {mkToken.(:greater_equal, [codepoint | String.to_charlist(peek)]), stream, line}

          _ ->
            {mkToken.(:greater, codepoint), stream, line}
        end

      _ when codepoint in [?\s, ?\r, ?\t] ->
        scan(stream, line)

      ?/ ->
        {peek, stream} = PeekableStream.peek(stream)

        case peek do
          "/" ->
            stream = consume_line(stream)
            scan(stream, line)

          _ ->
            {mkToken.(:slash, codepoint), stream, line}
        end

      _ when is_alpha(codepoint) ->
        {token, stream} = identifier(stream, [codepoint], line)
        {token, stream, line}

      ?" ->
        {token, stream} = string(stream, [codepoint], line)
        {token, stream, line}

      _ when is_digit(codepoint) ->
        {token, stream} = number(stream, [codepoint], line)
        {token, stream, line}

      ?\n ->
        scan(stream, line + 1)
    end
  end

  defp identifier(stream, lexeme, line) do
    {peek, stream} = PeekableStream.peek(stream)
    codepoint = to_codepoint(peek)

    case peek do
      _ when is_alphanumeric(codepoint) ->
        {_, stream} = PeekableStream.next(stream)
        identifier(stream, [lexeme | String.to_charlist(peek)], line)

      _ ->
        lexeme_str = to_string(lexeme)

        if MapSet.member?(Token.keywords(), lexeme_str) do
          {%Token{
             type: String.to_atom(lexeme_str),
             lexeme: lexeme_str,
             line: line
           }, stream}
        else
          {%Token{
             type: :identifier,
             lexeme: lexeme_str,
             literal: lexeme_str,
             line: line
           }, stream}
        end
    end
  end

  defp string(stream, lexeme, line) do
    {peek, stream} = PeekableStream.peek(stream)

    case peek do
      "\"" ->
        {_, stream} = PeekableStream.next(stream)
        [_ | literal] = List.flatten(lexeme)

        {%Token{
           type: :string,
           literal: to_string(literal),
           lexeme: [lexeme | String.to_charlist(peek)] |> to_string(),
           line: line
         }, stream}

      :eof ->
        # TODO probably should bubble up a user error for this rather than raise
        # an exception
        raise("unterminated string")

      _ ->
        {_, stream} = PeekableStream.next(stream)
        string(stream, [lexeme | String.to_charlist(peek)], line)
    end
  end

  defp number(stream, lexeme, line) do
    {peek, stream} = PeekableStream.peek(stream)
    codepoint = to_codepoint(peek)

    case peek do
      _ when is_digit(codepoint) ->
        {_, stream} = PeekableStream.next(stream)
        number(stream, [lexeme | String.to_charlist(peek)], line)

      "." ->
        {dot, stream} = PeekableStream.next(stream)

        {peekNext, stream} = PeekableStream.peek(stream)
        nextCodepoint = to_codepoint(peekNext)

        case peekNext do
          _ when is_digit(nextCodepoint) ->
            {_, stream} = PeekableStream.next(stream)
            lexeme = [lexeme | String.to_charlist(dot)]
            lexeme = [lexeme | String.to_charlist(peekNext)]
            number(stream, lexeme, line)

          _ ->
            # TODO probably should bubble up a user error for this rather than raise
            # an exception
            raise("malformed number")
        end

      _ ->
        {literal, ""} = Float.parse(to_string(lexeme))

        {%Token{
           type: :number,
           lexeme: to_string(lexeme),
           literal: literal,
           line: line
         }, stream}
    end
  end

  # TODO dedupe all of these. Maybe this is what agents and genservers are for.
  defp send_msg(pid, kind) do
    send(pid, {kind, self()})

    receive do
      {^kind, ^pid, char} -> char
    end
  end

  defp consume_line(stream) do
    {peek, stream} = PeekableStream.peek(stream)

    case peek do
      char when char in [:eof, "\n"] ->
        stream

      _ ->
        {_, stream} = PeekableStream.next(stream)
        consume_line(stream)
    end
  end

  defp to_codepoint(char) do
    [point] = String.to_charlist(char)
    point
  end
end
