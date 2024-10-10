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
    char = PeekableStream.next(stream)
    codepoint = to_codepoint(char)

    mkToken = fn type, char ->
      %Token{type: type, lexeme: char, line: Counter.value(line)}
    end

    case codepoint do
      ?( ->
        mkToken.(:left_paren, char)

      ?) ->
        mkToken.(:right_paren, char)

      ?{ ->
        mkToken.(:left_brace, char)

      ?} ->
        mkToken.(:right_brace, char)

      ?[ ->
        mkToken.(:left_bracket, char)

      ?] ->
        mkToken.(:right_bracket, char)

      ?, ->
        mkToken.(:comma, char)

      ?. ->
        mkToken.(:dot, char)

      ?- ->
        mkToken.(:minus, char)

      ?+ ->
        mkToken.(:plus, char)

      ?; ->
        mkToken.(:semicolon, char)

      ?* ->
        mkToken.(:star, char)

      ?! ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:bang_equal, char <> peek)

          _ ->
            mkToken.(:bang, char)
        end

      ?= ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:equal_equal, char <> peek)

          _ ->
            mkToken.(:equal, char)
        end

      ?< ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:less_equal, char <> peek)

          _ ->
            mkToken.(:less, char)
        end

      ?> ->
        case peek = PeekableStream.peek(stream) do
          "=" ->
            PeekableStream.next(stream)
            mkToken.(:greater_equal, char <> peek)

          _ ->
            mkToken.(:greater, char)
        end

      _ when codepoint in [?\s, ?\r, ?\t] ->
        scan(stream, line)

      ?/ ->
        case PeekableStream.peek(stream) do
          "/" ->
            consume_line(stream)
            scan(stream, line)

          _ ->
            mkToken.(:slash, char)
        end

      _ when is_alpha(codepoint) ->
        identifier(stream, char, line)

      ?" ->
        string(stream, char, line)

      _ when is_digit(codepoint) ->
        number(stream, char, line)

      ?\n ->
        Counter.increment(line)
        scan(stream, line)

      # TODO the mess I've made here is super confusing, using :eof to represent
      # a char, then converting back and forth. Maybe the PeekableStream here
      # should yield codepoint integers instead.
      0 ->
        :eof
    end
  end

  defp identifier(stream, lexeme, line) do
    peek = PeekableStream.peek(stream)
    codepoint = to_codepoint(peek)

    case peek do
      _ when is_alphanumeric(codepoint) ->
        PeekableStream.next(stream)
        identifier(stream, lexeme <> peek, line)

      _ ->
        if MapSet.member?(Token.keywords(), lexeme) do
          %Token{type: String.to_atom(lexeme), lexeme: lexeme, line: Counter.value(line)}
        else
          %Token{type: :identifier, lexeme: lexeme, literal: lexeme, line: Counter.value(line)}
        end
    end
  end

  defp string(stream, lexeme, line) do
    peek = PeekableStream.peek(stream)

    case peek do
      "\"" ->
        PeekableStream.next(stream)
        [_ | literal] = String.to_charlist(lexeme)

        %Token{
          type: :string,
          literal: to_string(literal),
          lexeme: lexeme <> peek,
          line: Counter.value(line)
        }

      :eof ->
        # TODO probably should bubble up a user error for this rather than raise
        # an exception
        raise("unterminated string")

      _ ->
        PeekableStream.next(stream)
        string(stream, lexeme <> peek, line)
    end
  end

  defp number(stream, lexeme, line) do
    peek = PeekableStream.peek(stream)
    codepoint = to_codepoint(peek)

    case peek do
      _ when is_digit(codepoint) ->
        PeekableStream.next(stream)
        number(stream, lexeme <> peek, line)

      "." ->
        dot = PeekableStream.next(stream)

        peekNext = PeekableStream.peek(stream)
        nextCodepoint = to_codepoint(peekNext)

        case peekNext do
          _ when is_digit(nextCodepoint) ->
            PeekableStream.next(stream)
            number(stream, lexeme <> dot <> peekNext, line)

          _ ->
            # TODO probably should bubble up a user error for this rather than raise
            # an exception
            raise("malformed number")
        end

      _ ->
        {literal, ""} = Float.parse(lexeme)

        %Token{
          type: :number,
          lexeme: lexeme,
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
    case char do
      :eof ->
        0

      _ ->
        [point] = String.to_charlist(char)
        point
    end
  end
end
