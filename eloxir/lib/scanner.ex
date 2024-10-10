defmodule Scanner do
  defguardp is_alpha(char)
            when is_integer(char) and (char in ?a..?z or char in ?A..?Z or char == ?_)

  defguardp is_digit(char)
            when is_integer(char) and char in ?0..?9

  defguardp is_alphanumeric(char)
            when is_alpha(char) or is_digit(char)

  def new(iodev) do
    stream = PeekableStream.new(IO.stream(iodev, 1))
    spawn_link(fn -> loop(stream, Counter.start(1)) end)
  end

  def next_token(pid), do: send_msg(pid, :next)
  def stop(pid), do: send(pid, :stop)

  defp loop(stream, line) do
    receive do
      {:next, caller} ->
        send(caller, {:next, self(), scan(stream, line)})
        loop(stream, line)
    end
  end

  defp scan(stream, line) do
    case char = PeekableStream.next(stream) do
      :eof -> :eof
      _ -> scan_char(stream, line, to_codepoint(char))
    end
  end

  defp scan_char(stream, line, codepoint) do
    mkToken = fn type, codepoint ->
      %Token{type: type, lexeme: to_string([codepoint]), line: Counter.value(line)}
    end

    case codepoint do
      ?( ->
        mkToken.(:left_paren, codepoint)

      ?) ->
        mkToken.(:right_paren, codepoint)

      ?{ ->
        mkToken.(:left_brace, codepoint)

      ?} ->
        mkToken.(:right_brace, codepoint)

      ?[ ->
        mkToken.(:left_bracket, codepoint)

      ?] ->
        mkToken.(:right_bracket, codepoint)

      ?, ->
        mkToken.(:comma, codepoint)

      ?. ->
        mkToken.(:dot, codepoint)

      ?- ->
        mkToken.(:minus, codepoint)

      ?+ ->
        mkToken.(:plus, codepoint)

      ?; ->
        mkToken.(:semicolon, codepoint)

      ?* ->
        mkToken.(:star, codepoint)

      ?! ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:bang_equal, [codepoint | String.to_charlist(peek)])

          _ ->
            mkToken.(:bang, codepoint)
        end

      ?= ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:equal_equal, [codepoint | String.to_charlist(peek)])

          _ ->
            mkToken.(:equal, codepoint)
        end

      ?< ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:less_equal, [codepoint | String.to_charlist(peek)])

          _ ->
            mkToken.(:less, codepoint)
        end

      ?> ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:greater_equal, [codepoint | String.to_charlist(peek)])

          _ ->
            mkToken.(:greater, codepoint)
        end

      _ when codepoint in [?\s, ?\r, ?\t] ->
        scan(stream, line)

      ?/ ->
        case PeekableStream.peek(stream) do
          "/" ->
            consume_line(stream)
            scan(stream, line)

          _ ->
            mkToken.(:slash, codepoint)
        end

      _ when is_alpha(codepoint) ->
        identifier(stream, [codepoint], line)

      ?" ->
        string(stream, [codepoint], line)

      _ when is_digit(codepoint) ->
        number(stream, [codepoint], line)

      ?\n ->
        Counter.increment(line)
        scan(stream, line)
    end
  end

  defp identifier(stream, lexeme, line) do
    peek = PeekableStream.peek(stream)
    codepoint = to_codepoint(peek)

    case peek do
      _ when is_alphanumeric(codepoint) ->
        PeekableStream.next(stream)
        identifier(stream, [lexeme | String.to_charlist(peek)], line)

      _ ->
        lexeme_str = to_string(lexeme)

        if MapSet.member?(Token.keywords(), lexeme_str) do
          %Token{
            type: String.to_atom(lexeme_str),
            lexeme: lexeme_str,
            line: Counter.value(line)
          }
        else
          %Token{
            type: :identifier,
            lexeme: lexeme_str,
            literal: lexeme_str,
            line: Counter.value(line)
          }
        end
    end
  end

  defp string(stream, lexeme, line) do
    peek = PeekableStream.peek(stream)

    case peek do
      "\"" ->
        PeekableStream.next(stream)
        [_ | literal] = List.flatten(lexeme)

        %Token{
          type: :string,
          literal: to_string(literal),
          lexeme: [lexeme | String.to_charlist(peek)] |> to_string(),
          line: Counter.value(line)
        }

      :eof ->
        # TODO probably should bubble up a user error for this rather than raise
        # an exception
        raise("unterminated string")

      _ ->
        PeekableStream.next(stream)
        string(stream, [lexeme | String.to_charlist(peek)], line)
    end
  end

  defp number(stream, lexeme, line) do
    peek = PeekableStream.peek(stream)
    codepoint = to_codepoint(peek)

    case peek do
      _ when is_digit(codepoint) ->
        PeekableStream.next(stream)
        number(stream, [lexeme | String.to_charlist(peek)], line)

      "." ->
        dot = PeekableStream.next(stream)

        peekNext = PeekableStream.peek(stream)
        nextCodepoint = to_codepoint(peekNext)

        case peekNext do
          _ when is_digit(nextCodepoint) ->
            PeekableStream.next(stream)
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

        %Token{
          type: :number,
          lexeme: to_string(lexeme),
          literal: literal,
          line: Counter.value(line)
        }
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
    case PeekableStream.peek(stream) do
      char when char in [:eof, "\n"] ->
        nil

      _ ->
        PeekableStream.next(stream)
        consume_line(stream)
    end
  end

  defp to_codepoint(char) do
    [point] = String.to_charlist(char)
    point
  end
end
